package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.ability.ChooseTypeAi;
import forge.card.CardType;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * 2021 Heroes of the Realm (PH21): Byode, Inverse Sun, Oracle text checked against Scryfall 2026-10-20 (its only
 * ruling is the Commander RC's name-on-the-card legality ruling). A Universe is a Universes Beyond printing's
 * umbrella franchise and every other printing is the Magic Universe (user ruling). A Universewalker isn't a
 * planeswalker: it has no loyalty (CR 306.5), so it enters with no counters and survives at 0 (704.5i), but its
 * loyalty abilities follow 606.
 */
public class HeroesOfTheRealm2021Test extends AITest {

    private Card printing(final String name, final String set, final String number, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, number);
        AssertJUnit.assertNotNull(name + " (" + set + " " + number + ")", pc);
        return Card.fromPaperCard(pc, p);
    }

    private PaperCard paper(final String name, final String set, final String number) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, number);
        AssertJUnit.assertNotNull(name + " (" + set + " " + number + ")", pc);
        return pc;
    }

    private SpellAbility ability(final Card byode, final ApiType api, final Player p) {
        for (final SpellAbility sa : byode.getSpellAbilities()) {
            if (sa.getApi() == api) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError("Byode has no " + api + " ability");
    }

    @Test
    public void testUniverseComesFromThePrinting() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertEquals("Warhammer", printing("Herald of Slaanesh", "40K", "77", p).getUniverse());
        AssertJUnit.assertEquals("Monster Hunter", printing("Tooth and Nail", "SLD", "2250", p).getUniverse());
        AssertJUnit.assertEquals("Magic", printing("Lightning Bolt", "M10", "146", p).getUniverse());
        AssertJUnit.assertEquals("Byode", printing("Byode, Inverse Sun", "PH21", "3", p).getUniverse());
    }

    @Test
    public void testByodeIsAUniversewalkerWithoutLoyalty() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card byode = addCard("Byode, Inverse Sun", p);
        AssertJUnit.assertTrue(byode.getType().hasType(CardType.CoreType.Universewalker));
        AssertJUnit.assertTrue(byode.isPermanent());
        AssertJUnit.assertFalse(byode.isPlaneswalker());
        AssertJUnit.assertTrue(byode.getType().hasSubtype("Byode"));
        AssertJUnit.assertEquals(0, byode.getCounters(CounterEnumType.LOYALTY));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(byode).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testPlusTwoReturnsAtMostOneCardPerUniverse() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card byode = addCard("Byode, Inverse Sun", p);
        final Card bolt = printing("Lightning Bolt", "M10", "146", p);
        final Card divination = printing("Divination", "M10", "49", p);
        final Card herald = printing("Herald of Slaanesh", "40K", "77", p);
        for (final Card c : Lists.newArrayList(bolt, divination, herald)) {
            p.getZone(ZoneType.Library).add(c, 0);
        }

        AbilityUtils.resolve(ability(byode, ApiType.Mill, p));
        final List<Card> returned = Lists.newArrayList();
        for (final Card c : Lists.newArrayList(bolt, divination, herald)) {
            final Card now = game.getCardState(c);
            AssertJUnit.assertTrue(c + " was milled", now.isInZone(ZoneType.Hand) || now.isInZone(ZoneType.Graveyard));
            if (now.isInZone(ZoneType.Hand)) {
                returned.add(now);
            }
        }
        AssertJUnit.assertFalse("both Magic cards came back", returned.contains(game.getCardState(bolt))
                && returned.contains(game.getCardState(divination)));
        AssertJUnit.assertTrue(returned.size() <= 2);
    }

    @Test
    public void testMinusXCastsAFreeSpellFromAnUnchosenUniverse() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card byode = addCard("Byode, Inverse Sun", p);
        byode.addCounterInternal(CounterEnumType.LOYALTY, 3, p, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        final Card herald = printing("Herald of Slaanesh", "40K", "77", p);
        p.getZone(ZoneType.Hand).add(herald);

        final SpellAbility minusX = ability(byode, ApiType.ChooseType, p);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) p.getController()).getAi().canPlaySa(minusX));
        AssertJUnit.assertEquals(Integer.valueOf(3), minusX.getXManaCostPaid());
        AbilityUtils.resolve(minusX);
        game.getStack().resolveStack();

        AssertJUnit.assertEquals("Warhammer", byode.getChosenType());
        AssertJUnit.assertTrue(Lists.newArrayList(byode.getNotedTypes()).contains("Warhammer"));
        AssertJUnit.assertTrue(game.getCardState(herald).isInZone(ZoneType.Battlefield));

        // a Universe chosen this way can't be chosen again, and nothing in hand fits 2 or less
        final Card another = printing("Herald of Slaanesh", "40K", "77", p);
        p.getZone(ZoneType.Hand).add(another);
        AssertJUnit.assertNull(ChooseTypeAi.byodeSpell(p, byode, 3));
        final Card divination = printing("Divination", "M10", "49", p);
        p.getZone(ZoneType.Hand).add(divination);
        AssertJUnit.assertNull(ChooseTypeAi.byodeSpell(p, byode, 2));
        AssertJUnit.assertEquals(divination, ChooseTypeAi.byodeSpell(p, byode, 3));
    }

    @Test
    public void testCommanderDeckTakesUniversesBeyondCardsOfAnyColor() {
        initAndCreateGame();
        final PaperCard byode = paper("Byode, Inverse Sun", "PH21", "3");
        AssertJUnit.assertTrue(DeckFormat.Commander.isLegalCommander(byode.getRules()));
        final var legal = DeckFormat.Commander.isLegalCardForCommanderPredicate(List.of(byode));
        AssertJUnit.assertTrue("red Universes Beyond printing", legal.test(paper("Herald of Slaanesh", "40K", "77")));
        AssertJUnit.assertFalse("red Magic printing", legal.test(paper("Lightning Bolt", "M10", "146")));
        AssertJUnit.assertTrue("blue Magic printing", legal.test(paper("Divination", "M10", "49")));
    }
}
