package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Loopy Lobster (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: four-faced
 * - it starts at stage 1 and moves on a stage each time it becomes tapped, back to 1 after 4; off the battlefield it's
 * a 0/0; its stage 3 pump lasts even if it moves to stage 4 that turn.
 */
public class LoopyLobsterTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private void tapAndMove(final Game game, final Card c) {
        c.untap();
        c.tap(true, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        resolveAll(game);
    }

    private static SpellAbility ability(final Card c, final ApiType api) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testFourStagesAndRoundAgain() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        fillLibrary(ai, 5);
        addCards("Island", 2, ai);
        Card inHand = addCardToZone("Loopy Lobster", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(0, inHand.getNetPower());
        AssertJUnit.assertEquals(0, inHand.getNetToughness());

        Card lobster = addCard("Loopy Lobster", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, lobster.getStage());
        AssertJUnit.assertEquals(1, lobster.getNetPower());
        AssertJUnit.assertEquals(4, lobster.getNetToughness());

        tapAndMove(game, lobster);
        AssertJUnit.assertEquals(2, lobster.getStage());
        AssertJUnit.assertEquals(4, lobster.getNetPower());
        AssertJUnit.assertEquals(1, lobster.getNetToughness());
        AssertJUnit.assertTrue(lobster.hasKeyword(Keyword.EVOLVE));

        tapAndMove(game, lobster);
        AssertJUnit.assertEquals(3, lobster.getStage());
        AssertJUnit.assertFalse(lobster.hasKeyword(Keyword.EVOLVE));
        SpellAbility pump = ability(lobster, ApiType.Pump);
        AssertJUnit.assertNotNull(pump);
        pump.setActivatingPlayer(ai);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(3, lobster.getNetPower());
        AssertJUnit.assertEquals(3, lobster.getNetToughness());

        // stage 4: 4/4 with vigilance and a draw, and the pump still applies
        tapAndMove(game, lobster);
        AssertJUnit.assertEquals(4, lobster.getStage());
        AssertJUnit.assertTrue(lobster.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertEquals(5, lobster.getNetPower());
        AssertJUnit.assertEquals(3, lobster.getNetToughness());
        AssertJUnit.assertNull(ability(lobster, ApiType.Pump));
        AssertJUnit.assertNotNull(ability(lobster, ApiType.Draw));

        tapAndMove(game, lobster);
        AssertJUnit.assertEquals(1, lobster.getStage());
        AssertJUnit.assertNull(ability(lobster, ApiType.Draw));
    }

    @Test
    public void testANewObjectStartsOver() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card lobster = addCard("Loopy Lobster", ai);
        game.getAction().checkStateEffects(true);
        tapAndMove(game, lobster);
        AssertJUnit.assertEquals(2, lobster.getStage());

        Card back = game.getAction().moveToHand(lobster, null);
        Card again = game.getAction().moveToPlay(back, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, again.getStage());
    }
}
