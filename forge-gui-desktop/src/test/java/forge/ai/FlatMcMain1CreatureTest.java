package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A creature that gains nothing from the first main phase waits for the second: with Flat Monte Carlo off, with the
 * phone settings, and with the desktop maximum, whose broad discovery used to offer it before combat anyway.
 */
public class FlatMcMain1CreatureTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    @DataProvider(name = "modes")
    public Object[][] modes() {
        return new Object[][] { { "off" }, { "lite" }, { "max" } };
    }

    private Game newGame(final String mode) {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = !"off".equals(mode);
        game.AI_FLAT_MONTE_CARLO_LITE = "lite".equals(mode);
        game.AI_FLAT_MONTE_CARLO_MAX = "max".equals(mode);
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    /** Untapped lands, two bears that can attack an empty board, and one creature in hand. */
    private Player boardWithAttackers(final Game game, final String inHand) {
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Forest", 3, ai);
        addCards("Mountain", 1, ai);
        for (final Card bear : addCards("Grizzly Bears", 2, ai)) {
            bear.setSickness(false);
        }
        addCardToZone(inHand, ai, ZoneType.Hand);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
            addCardToZone("Mountain", opp, ZoneType.Library);
        }
        setPhase(game, PhaseType.MAIN1, ai);
        return ai;
    }

    private static void setPhase(final Game game, final PhaseType phase, final Player turnPlayer) {
        game.getPhaseHandler().devModeSet(phase, turnPlayer);
        game.getAction().checkStateEffects(true);
    }

    private static AiController brainOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private static String nextPlay(final Player ai) {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void vanillaCreatureWaitsForTheSecondMainPhase(final String mode) {
        final Game game = newGame(mode);
        final Player ai = boardWithAttackers(game, "Runeclaw Bear");
        AssertJUnit.assertNull(mode + ": nothing is gained by casting the bear before combat", nextPlay(ai));
        AssertJUnit.assertNull(mode + ": no rollouts for a creature the heuristics hold",
                brainOf(ai).getLastFlatMonteCarloResult());
        setPhase(game, PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(mode + ": after combat the bear is cast", "Runeclaw Bear", nextPlay(ai));
    }

    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void hastyCreatureIsStillCastBeforeCombat(final String mode) {
        final Game game = newGame(mode);
        final Player ai = boardWithAttackers(game, "Raging Goblin");
        AssertJUnit.assertEquals(mode + ": haste is what the first main phase is for", "Raging Goblin", nextPlay(ai));
    }
}
