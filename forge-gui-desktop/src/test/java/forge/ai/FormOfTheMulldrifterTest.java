package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Form of the Mulldrifter (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall
 * 2026-10-10: creature cards in your hand may be cast as though they were Mulldrifter, evoke included; a card cast this
 * way has Mulldrifter's characteristics (as copiable values) until it leaves the battlefield or leaves the stack other
 * than by resolving, and stays a Mulldrifter if the Form leaves.
 */
public class FormOfTheMulldrifterTest extends AITest {

    private static SpellAbility castAs(final Card card, final Player p, final boolean evoke) {
        for (final SpellAbility sa : card.getAllPossibleAbilities(p, true)) {
            if ("Mulldrifter".equals(sa.getCastAs()) && sa.isEvoke() == evoke) {
                return sa;
            }
        }
        return null;
    }

    /** Resolves the stack, then the triggers that resolving set off, until nothing's left. */
    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    @Test
    public void testCastAsMulldrifter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Form of the Mulldrifter", ai);
        addCards("Island", 5, ai);
        fillLibrary(ai, 5);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertNull(castAs(bolt, ai, false));
        SpellAbility sa = castAs(bears, ai, false);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertNotNull(castAs(bears, ai, true));

        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        Card onStack = game.getCardState(bears);
        AssertJUnit.assertTrue(onStack.isInZone(ZoneType.Stack));
        AssertJUnit.assertEquals("Mulldrifter", onStack.getName());
        AssertJUnit.assertEquals(5, onStack.getCMC());

        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        resolveAll(game);
        Card drifter = game.getCardState(bears);
        AssertJUnit.assertTrue(drifter.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("Mulldrifter", drifter.getName());
        AssertJUnit.assertTrue(drifter.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertEquals(2, drifter.getNetPower());
        AssertJUnit.assertEquals(hand + 2, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testEvokeAsMulldrifter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Form of the Mulldrifter", ai);
        addCards("Island", 3, ai);
        fillLibrary(ai, 5);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility evoke = castAs(bears, ai, true);
        AssertJUnit.assertNotNull(evoke);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, evoke, null));
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        resolveAll(game);

        // sacrificed as it entered, having drawn two; in the graveyard it's just Grizzly Bears again
        Card gone = game.getCardState(bears);
        AssertJUnit.assertTrue(gone.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Grizzly Bears", gone.getName());
        AssertJUnit.assertEquals(hand + 2, ai.getCardsIn(ZoneType.Hand).size());
    }
}
