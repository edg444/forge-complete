package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.GuessNameAi;
import forge.card.CardFlavorText;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CountersMoveEffect;
import forge.game.card.Card;
import forge.game.card.CardProperty;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Everythingamajig's six printings (Unstable 147a-f). Oracle text and rulings checked against Scryfall
 * 2026-10-09, plus the Unstable FAQ's Everythingamajig section.
 */
public class EverythingamajigTest extends AITest {

    private static final String[] CNS = {"147a", "147b", "147c", "147d", "147e", "147f"};

    private Card printing(String cn, Player p) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Everythingamajig", "UST", cn);
        AssertJUnit.assertNotNull(cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        AssertJUnit.fail(c + " has no " + api + " ability");
        return null;
    }

    @Test
    public void testEveryPrintingHasItsOwnAbilities() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        int[] costs = {5, 5, 5, 6, 5, 8};
        String[] firstLines = {"{2}, {T}: Move a counter", "{2}, {T}: Draw a card.", "{1}: Flip a coin.",
                "{T}: Add one mana of any color.", "Sacrifice a land: You gain 2 life.", "{1}, {T}: Scry 2."};
        for (int i = 0; i < CNS.length; i++) {
            Card c = printing(CNS[i], me);
            AssertJUnit.assertEquals(CNS[i], costs[i], c.getCMC());
            AssertJUnit.assertTrue(CNS[i], c.getColor().isColorless());
            AssertJUnit.assertTrue(CNS[i] + ": " + c.getOracleText(), c.getOracleText().startsWith(firstLines[i]));
            AssertJUnit.assertEquals(CNS[i], 3, c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).count());
        }
    }

    @Test
    public void testFlipIsAManaAbilityButNotForPayments() {
        Game game = initAndCreateGame();
        Card c = printing("147c", game.getPlayers().get(1));
        SpellAbility flip = ability(c, ApiType.FlipCoin);
        // CR 605.1a: no target, could add mana - it doesn't use the stack
        AssertJUnit.assertTrue(flip.isManaAbility());
        AssertJUnit.assertTrue(flip.getRestrictions().isInstantSpeed());
    }

    @Test
    public void testMoveTakesAnUntargetableCounter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card fan = printing("147a", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card scout = addCard("Gladecover Scout", opp);
        scout.setCounters(CounterEnumType.P1P1, 2);
        AssertJUnit.assertTrue(scout.hasKeyword(Keyword.HEXPROOF));
        // like any reusable tap ability, the AI saves it for the end of the opponent's turn
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility move = ability(fan, ApiType.MoveCounter);
        move.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(move.usesTargeting());
        boolean willing = false;
        // the AI passes on a reusable ability one time in five at random
        for (int i = 0; i < 30 && !willing; i++) {
            willing = SpellApiToAi.Converter.get(move).canPlayWithSubs(ai, move).willingToPlay();
        }
        AssertJUnit.assertTrue(willing);
        AbilityUtils.resolve(move);

        AssertJUnit.assertEquals(1, scout.getCounters(CounterEnumType.P1P1));
        // Grizzly Bears refers to no kind of counter, so it's a +1/+1 counter
        AssertJUnit.assertEquals(1, bears.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testAiLeavesHelpfulCountersAlone() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card fan = printing("147a", ai);
        addCard("Grizzly Bears", ai).setCounters(CounterEnumType.P1P1, 1);
        addCard("Grizzly Bears", opp).setCounters(CounterEnumType.M1M1, 1);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility move = ability(fan, ApiType.MoveCounter);
        move.setActivatingPlayer(ai);
        for (int i = 0; i < 30; i++) {
            AssertJUnit.assertFalse(SpellApiToAi.Converter.get(move).canPlayWithSubs(ai, move).willingToPlay());
        }
    }

    @Test
    public void testMovedCounterBecomesAKindTheDestinationNames() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card fan = printing("147a", ai);
        // Chronozoa's text names time counters (vanishing's reminder text) and nothing else
        Card chronozoa = addCard("Chronozoa", ai);
        Card bears = addCard("Grizzly Bears", opp);
        bears.setCounters(CounterEnumType.P1P1, 1);
        game.getAction().checkStateEffects(true);
        final int time = chronozoa.getCounters(CounterEnumType.TIME);

        SpellAbility move = ability(fan, ApiType.MoveCounter);
        move.setActivatingPlayer(ai);
        AbilityUtils.resolve(move);

        AssertJUnit.assertEquals(0, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(time + 1, chronozoa.getCounters(CounterEnumType.TIME));
        AssertJUnit.assertEquals(0, chronozoa.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testReferredCounterKinds() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        List<CounterType> bears = CountersMoveEffect.referredCounterTypes(addCard("Grizzly Bears", me));
        AssertJUnit.assertTrue(bears.isEmpty());
        // FAQ: a planeswalker's loyalty costs are symbols that refer to loyalty counters
        List<CounterType> jace = CountersMoveEffect.referredCounterTypes(addCard("Jace Beleren", me));
        AssertJUnit.assertTrue(jace.contains(CounterEnumType.LOYALTY));
        // two kinds named: the player picks between them
        List<CounterType> melira = CountersMoveEffect.referredCounterTypes(addCard("Melira, Sylvok Outcast", me));
        AssertJUnit.assertEquals(List.of(CounterEnumType.M1M1, CounterEnumType.POISON), melira);
    }

    @Test
    public void testCompleteWordsKeepHyphensAndPossessives() {
        // FAQ: discarding Five-Finger Discount can't find Five-Alarm Fire
        AssertJUnit.assertFalse(shares("Five-Finger Discount", "Five-Alarm Fire"));
        AssertJUnit.assertFalse(shares("Urza's Hot Tub", "Kaya's Guile"));
        AssertJUnit.assertTrue(shares("Urza's Hot Tub", "Urza's Mine"));
        AssertJUnit.assertTrue(shares("Ral, Izzet Viceroy", "Ral Zarek"));
        AssertJUnit.assertTrue(shares("Fire // Ice", "Five-Alarm Fire"));
    }

    private static boolean shares(String a, String b) {
        java.util.Set<String> words = CardProperty.completeWords(a);
        return CardProperty.completeWords(b).stream().anyMatch(words::contains);
    }

    @Test
    public void testLibrariesAndGraveyardsChangeHandsInOrder() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        String[] mine = {"Island", "Forest", "Mountain"};
        String[] theirs = {"Plains", "Swamp"};
        for (String n : mine) {
            addCardToZone(n, me, ZoneType.Library);
            addCardToZone(n, me, ZoneType.Graveyard);
        }
        for (String n : theirs) {
            addCardToZone(n, opp, ZoneType.Library);
            addCardToZone(n, opp, ZoneType.Graveyard);
        }
        List<Card> myLibrary = List.copyOf(me.getZone(ZoneType.Library).getCards());
        List<Card> myGraveyard = List.copyOf(me.getZone(ZoneType.Graveyard).getCards());
        List<Card> theirLibrary = List.copyOf(opp.getZone(ZoneType.Library).getCards());
        Card host = printing("147f", me);

        for (String zone : new String[] {"Library", "Graveyard"}) {
            SpellAbility swap = AbilityFactory.getAbility("DB$ ExchangeZoneAll | Defined$ Player | Zone$ " + zone, host);
            swap.setActivatingPlayer(me);
            AbilityUtils.resolve(swap);
        }

        AssertJUnit.assertEquals(myLibrary, List.copyOf(opp.getZone(ZoneType.Library).getCards()));
        AssertJUnit.assertEquals(theirLibrary, List.copyOf(me.getZone(ZoneType.Library).getCards()));
        AssertJUnit.assertEquals(myGraveyard, List.copyOf(opp.getZone(ZoneType.Graveyard).getCards()));
        // ruling: the receiving player owns them for the rest of the game
        for (Card c : myLibrary) {
            AssertJUnit.assertEquals(opp, c.getOwner());
        }
    }

    @Test
    public void testRecallFindsTheCardPrintingThatFlavorText() {
        initAndCreateGame();
        PaperCard withFlavor = null;
        String flavor = "";
        for (PaperCard pc : FModel.getMagicDb().getCommonCards().getAllCards("Grizzly Bears")) {
            flavor = CardFlavorText.get(pc.getEdition(), pc.getCollectorNumber());
            if (!flavor.isEmpty()) {
                withFlavor = pc;
                break;
            }
        }
        AssertJUnit.assertNotNull("no Grizzly Bears printing with flavor text", withFlavor);
        AssertJUnit.assertTrue(GuessNameAi.namesPrinting(flavor).contains("Grizzly Bears"));
    }
}
