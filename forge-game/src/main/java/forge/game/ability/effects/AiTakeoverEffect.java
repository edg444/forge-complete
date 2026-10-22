package forge.game.ability.effects;

import java.util.function.Supplier;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;

/**
 * The Egotistical Velociraptor: another Magic player takes over your game until the end of your next turn - in Forge
 * an AI, who always agrees (user ruling 2026-10-22). It's remembered on the host when it happens, for "if they agree".
 */
public class AiTakeoverEffect extends SpellAbilityEffect {

    /** Makes the AI that takes over; set by the GUI layer. Without it, an AI opponent's kind of player is used. */
    private static Supplier<IGameEntitiesFactory> aiSource = null;

    public static void setAiSource(final Supplier<IGameEntitiesFactory> source) {
        aiSource = source;
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Another player takes over " + sa.getActivatingPlayer() + "'s game until the end of their next turn.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        IGameEntitiesFactory ai = aiSource != null ? aiSource.get() : null;
        if (ai == null) {
            for (final Player opp : p.getOpponents()) {
                if (opp.getController().isAI() && opp.getLobbyPlayer() instanceof IGameEntitiesFactory factory) {
                    ai = factory;
                    break;
                }
            }
        }
        if (ai == null) {
            return;
        }
        final PlayerController pc = ai.createMindSlaveController(p, p);
        final long ts = game.getNextTimestamp();
        p.addController(ts, p, pc, true);
        game.getGameLog().add(GameLogEntryType.INFORMATION, "Another player takes over " + p + "'s game.");
        sa.getHostCard().addRemembered(p);
        addUntilCommand(sa, new GameCommand() {
            private static final long serialVersionUID = 1L;

            @Override
            public void run() {
                p.removeController(ts);
                game.getGameLog().add(GameLogEntryType.INFORMATION, p + " takes their game back.");
            }
        }, "UntilTheEndOfYourNextTurn", p);
    }
}
