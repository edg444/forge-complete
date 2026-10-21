package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Meet and Greet "Sisay" (UNF #172), Pietra, Crafter of Clowns (UNF #176), Autograph Book (UNF #183) and Blue Ribbon
 * (UNF #184), checked against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityMulticolorBatchTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private Card printing(final String name, final String set, final Player p, final ZoneType zone) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set);
        final Card c = Card.fromPaperCard(pc, p);
        p.getZone(zone).add(c);
        return c;
    }

    @Test
    public void testSisayFlavorOrHistory() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card sisay = addCard("Meet and Greet \"Sisay\"", ai);
        printing("Forest", "UNF", ai, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();
        final Card elves = printing("Llanowar Elves", "M12", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(3, elves.getPrintedFlavorLines());
        final SpellAbility cast = elves.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, cast, null));
        resolveAll(game);
        final boolean flavor = game.getCardState(sisay).getNetPower() == 4;
        final int treasures = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")).size();
        AssertJUnit.assertTrue("+3/+3 for three lines, or a Treasure: no UNF or M12 symbol shared", flavor ^ treasures == 1);
    }

    @Test
    public void testPietraClownRobots() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card pietra = addCard("Pietra, Crafter of Clowns", ai);
        game.getAction().checkStateEffects(true);
        SpellAbility ability = null;
        for (final SpellAbility sa : pietra.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                ability = sa;
            }
        }
        ability.setActivatingPlayer(ai);
        AbilityUtils.resolve(ability);
        game.getAction().checkStateEffects(true);
        final Card robot = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Robot")).get(0);
        AssertJUnit.assertEquals("Robots get +1/+1", 2, robot.getNetPower());
    }

    @Test
    public void testAutographBookPages() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card book = addCardToZone("Autograph Book", ai, ZoneType.Hand);
        game.getAction().moveToPlay(book, ai, null, AbilityKey.newMap());
        final int pages = game.getCardState(book).getCounters(CounterType.getType("PAGE"));
        AssertJUnit.assertTrue("a simulated book's signers", pages >= 1 && pages <= 5);
    }

    @Test
    public void testBlueRibbonBestInShow() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card winner = addCard("Grizzly Bears", ai);
        final Card ribbon = addCardToZone("Blue Ribbon", ai, ZoneType.Hand);
        game.getAction().moveToPlay(ribbon, ai, null, AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals("the only creature", winner, game.getCardState(ribbon).getEntityAttachedTo());

        final Card other = addCard("Llanowar Elves", ai);
        addCard("Forest", ai);
        game.getAction().checkStateEffects(true);
        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility pump = growth.getFirstSpellAbility();
        pump.setActivatingPlayer(ai);
        pump.getTargets().add(other);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, pump, null));
        resolveAll(game);
        AssertJUnit.assertEquals(4, other.getNetPower());
        AssertJUnit.assertEquals("the copy targets the ribbon's creature", 5, winner.getNetPower());
    }
}
