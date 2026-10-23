package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
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
 * Black Lotus Unknown Planechase (PUNK), second part, checked against Scryfall 2026-10-22 (no rulings). User rulings
 * 2026-10-22: worn pins and years played are said once per match (the AI's are random: 0-5 pins, 1 to this year - 1993
 * years); Shrinking Plane folds seven times; Windmill Farm's spin is a random permanent and a full turn 80% of the time;
 * the animal and horror-monster type lists are mine; D&D is the Forgotten Realms sets plus the D&D Secret Lairs.
 */
public class BlackLotusUnknownPlanechase2Test extends AITest {

    private static Card planeswalk(final Game game, final Player p, final String name) {
        final PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name);
        AssertJUnit.assertNotNull("no plane card " + name, pc);
        final Card plane = Card.fromPaperCard(pc, p);
        if (p.getZone(ZoneType.PlanarDeck).isEmpty()) {
            p.getZone(ZoneType.PlanarDeck).add(Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard("Bicycle Rack"), p));
        }
        p.getZone(ZoneType.PlanarDeck).add(plane, 0);
        p.leaveCurrentPlane();
        p.planeswalkTo(null, new CardCollection(plane));
        resolveAll(game);
        return game.getCardState(plane);
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
        game.getAction().checkStateEffects(true);
    }

    private static void chaos(final Game game, final Player p) {
        PlanarDice.roll(p, PlanarDice.Chaos);
        resolveAll(game);
    }

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private Game start() {
        final Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    private Card printing(final String name, final String set, final Player p) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        return c;
    }

    @Test
    public void testPins() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card booth = planeswalk(game, ai, "Pin Collector's Booth");
        final Card bear = addCard("Grizzly Bears", ai);
        final SpellAbility pin = svar(booth, "TrigPin", ai);
        pin.getTargets().add(bear);
        AbilityUtils.resolve(pin);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("the Pin token's +1/+1", 3, bear.getNetPower());
        final int life = human.getLife();
        chaos(game, ai);
        final int worn = forge.game.ability.effects.MatchNumberEffect.numberOf(ai, "PinsWorn");
        AssertJUnit.assertTrue("the AI wears 0-5", worn >= 0 && worn <= 5);
        AssertJUnit.assertEquals(life - worn - 1, human.getLife());
        chaos(game, ai);
        AssertJUnit.assertEquals("asked once per match", worn, forge.game.ability.effects.MatchNumberEffect.numberOf(ai, "PinsWorn"));

        fillLibrary(ai, 15);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final Card trading = planeswalk(game, ai, "Pin Trading");
        AssertJUnit.assertFalse(trading.isInZone(ZoneType.Command));
        AssertJUnit.assertEquals(hand + worn + 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testShrinkingPlane() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 40);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final Card plane = planeswalk(game, ai, "Shrinking Plane");
        AssertJUnit.assertEquals(1, plane.getCounters(CounterType.getType("FOLD")));
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        for (int i = 2; i <= 7; i++) {
            AbilityUtils.resolve(svar(plane, "TrigFold", ai));
        }
        AssertJUnit.assertEquals(7, plane.getCounters(CounterType.getType("FOLD")));
        AssertJUnit.assertEquals("1+2+...+7", hand + 28, ai.getCardsIn(ZoneType.Hand).size());
        AbilityUtils.resolve(svar(plane, "TrigFold", ai));
        resolveAll(game);
        AssertJUnit.assertEquals("seven more, then it can't fold", hand + 35, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertFalse(game.getCardState(plane).isInZone(ZoneType.Command));

        final Card again = planeswalk(game, ai, "Shrinking Plane");
        for (int i = 0; i < 2; i++) {
            addCardToZone("Island", human, ZoneType.Hand);
        }
        final int theirs = human.getCardsIn(ZoneType.Hand).size();
        chaos(game, ai);
        AssertJUnit.assertEquals(0, again.getCounters(CounterType.getType("FOLD")));
        AssertJUnit.assertEquals(theirs - 2, human.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testWindmillFarm() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        planeswalk(game, ai, "Windmill Farm");
        final Card petal = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Tulip Petal")).get(0);
        AssertJUnit.assertTrue(petal.isArtifact() && petal.isToken());
        AssertJUnit.assertEquals(1, petal.getManaAbilities().size());
        addCard("Grizzly Bears", ai);
        int copies = 0;
        for (int i = 0; i < 20; i++) {
            final int before = ai.getCardsIn(ZoneType.Battlefield).size();
            chaos(game, ai);
            copies += ai.getCardsIn(ZoneType.Battlefield).size() - before;
        }
        AssertJUnit.assertTrue("most spins go all the way around: " + copies, copies >= 8 && copies <= 20);
    }

    @Test
    public void testNewPlayersJourney() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        addCardToZone("Forest", ai, ZoneType.Library);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        planeswalk(game, ai, "New Player's Journey");
        AssertJUnit.assertEquals("the AI takes its basic land", hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        final Card plane = game.getCardState(CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().equals("New Player's Journey")).get(0));
        AssertJUnit.assertTrue(printing("Llanowar Elves", "LEA", ai).isValid("Card.CorePrinting", ai, plane, null));
        AssertJUnit.assertTrue(printing("Llanowar Elves", "FDN", ai).isValid("Card.CorePrinting", ai, plane, null));
        AssertJUnit.assertFalse(printing("Llanowar Elves", "DOM", ai).isValid("Card.CorePrinting", ai, plane, null));

        final int life = ai.getLife();
        chaos(game, ai);
        final int years = forge.game.ability.effects.MatchNumberEffect.numberOf(ai, "YearsPlayed");
        AssertJUnit.assertTrue(years >= 1 && years <= java.time.LocalDate.now().getYear() - 1993);
        AssertJUnit.assertEquals(life + years, ai.getLife());
        AssertJUnit.assertFalse("then it planeswalks", game.getCardState(plane).isInZone(ZoneType.Command));
    }

    @Test
    public void testWelcomeToValley() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card plane = planeswalk(game, ai, "Welcome to Valley");
        final Card outcast = addCardToZone("Changeling Outcast", ai, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(addCardToZone("Grizzly Bears", ai, ZoneType.Hand).isValid("Card.CreatureTypeListedIn_AnimalTypes", ai, plane, null));
        AssertJUnit.assertFalse("a Human isn't an animal", addCardToZone("Elite Vanguard", ai, ZoneType.Hand)
                .isValid("Card.CreatureTypeListedIn_AnimalTypes", ai, plane, null));
        AssertJUnit.assertTrue("changelings are every type", outcast.isValid("Card.CreatureTypeListedIn_AnimalTypes", ai, plane, null));
        final Card giant = addCard("Hill Giant", ai);
        final SpellAbility copy = svar(plane, "TrigCopy", ai);
        copy.getTargets().add(giant);
        AbilityUtils.resolve(copy);
        final Card token = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getName().equals("Hill Giant")).get(0);
        AssertJUnit.assertEquals(1, token.getNetPower());
        AssertJUnit.assertEquals(1, token.getNetToughness());
    }

    @Test
    public void testSorinsRemasteredManor() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bear = addCard("Grizzly Bears", ai);
        final Card vampire = addCard("Vampire Interloper", ai);
        planeswalk(game, ai, "Sorin's Remastered Manor");
        AssertJUnit.assertEquals("its only creature gets the counter", 1, bear.getCounters(CounterType.getType("SPOOKY")) + vampire.getCounters(CounterType.getType("SPOOKY")));
        AssertJUnit.assertTrue("a Vampire is spooky anyway", vampire.hasKeyword(Keyword.MENACE));
        AssertJUnit.assertEquals(3, vampire.getNetPower());
        final int life = ai.getLife();
        final int theirs = human.getLife();
        chaos(game, ai);
        final int dealt = theirs - human.getLife();
        AssertJUnit.assertTrue("a spooky creature bit them: " + dealt, dealt == 3 || dealt == bear.getNetPower());
        AssertJUnit.assertEquals(life + dealt, ai.getLife());
    }

    @Test
    public void testMultiversalHighCouncil() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card plane = planeswalk(game, ai, "Multiversal High Council");
        final Card magic = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("one Universe", 3, magic.getNetPower());
        final Card dnd = printing("Owlbear", "AFR", ai);
        ai.getZone(ZoneType.Battlefield).add(dnd);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(forge.card.PrintingTraits.DND_UNIVERSE, dnd.getCouncilUniverse());
        AssertJUnit.assertEquals("Magic and D&D", 4, magic.getNetPower());
        final Card lair = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Ponder", "SLD", "1783"), ai);
        AssertJUnit.assertEquals("a D&D Secret Lair", forge.card.PrintingTraits.DND_UNIVERSE, lair.getCouncilUniverse());
        AssertJUnit.assertEquals("not every Secret Lair", forge.card.PrintingTraits.MAGIC_UNIVERSE,
                Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Ponder", "SLD", "245"), ai).getCouncilUniverse());
        final Card frodo = printing("Frodo Baggins", "LTR", ai);
        AssertJUnit.assertTrue(frodo.isValid("Card.UniversesBeyond", ai, plane, null));
        AssertJUnit.assertFalse(dnd.isValid("Card.UniversesBeyond", ai, plane, null));

        final Card yardMagic = addCardToZone("Llanowar Elves", ai, ZoneType.Graveyard);
        final Card yardMagic2 = addCardToZone("Hill Giant", ai, ZoneType.Graveyard);
        final Card yardDnd = printing("Owlbear", "AFR", ai);
        ai.getZone(ZoneType.Graveyard).add(yardDnd);
        final SpellAbility ret = svar(plane, "TrigReturn", ai);
        AssertJUnit.assertTrue(ret.canTarget(yardMagic));
        ret.getTargets().add(yardMagic);
        AssertJUnit.assertFalse("one per Universe", ret.canTarget(yardMagic2));
        AssertJUnit.assertTrue(ret.canTarget(yardDnd));
    }

    @Test
    public void testStroopwafelCafe() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(human, 5);
        planeswalk(game, ai, "Stroopwafel Cafe");
        final Card top = ai.getCardsIn(ZoneType.Library).get(0);
        AssertJUnit.assertTrue(top.isArtifact());
        AssertJUnit.assertTrue(top.getType().hasSubtype("Food"));
        AssertJUnit.assertFalse("only the top card", ai.getCardsIn(ZoneType.Library).get(1).isArtifact());
        AssertJUnit.assertTrue(top.mayPlayerLook(ai));
        AssertJUnit.assertFalse(top.mayPlayerLook(human));
        final Card theirTop = human.getCardsIn(ZoneType.Library).get(0);
        AssertJUnit.assertTrue(theirTop.mayPlayerLook(human));
        SpellAbility food = null;
        for (final SpellAbility sa : top.getSpellAbilities()) {
            if (sa.getApi() == forge.game.ability.ApiType.GainLife) {
                food = sa;
            }
        }
        AssertJUnit.assertNotNull(food);
        final int life = ai.getLife();
        food.setActivatingPlayer(ai);
        AbilityUtils.resolve(food);
        AssertJUnit.assertEquals(life + 3, ai.getLife());
    }

    @Test
    public void testMadLabs() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        addCard("Sol Ring", ai);
        addCard("Forest", ai);
        addCard("Glorious Anthem", ai);
        // no planeswalker: they're all legendary, and the AI's copy of its own would go to the legend rule
        game.getAction().checkStateEffects(true);
        final int before = ai.getCardsIn(ZoneType.Battlefield).size() + human.getCardsIn(ZoneType.Battlefield).size();
        final Card labs = planeswalk(game, ai, "Mad Labs");
        AssertJUnit.assertFalse(labs.isInZone(ZoneType.Command));
        final java.util.List<Card> tokens = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Card::isToken);
        if (tokens.isEmpty()) {
            // the random permanent type was Battle or Planeswalker, and there are none
            AssertJUnit.assertEquals(before, ai.getCardsIn(ZoneType.Battlefield).size() + human.getCardsIn(ZoneType.Battlefield).size());
            return;
        }
        AssertJUnit.assertEquals("each player made one", 2, tokens.size());
        for (final Card t : tokens) {
            AssertJUnit.assertTrue(t.isCreature());
            AssertJUnit.assertTrue(t.getCounters(CounterEnumType.P1P1) >= 1);
            AssertJUnit.assertTrue("a keyword counter", t.getCounters().elementSet().stream().anyMatch(c -> c.isKeywordCounter()));
        }
    }
}
