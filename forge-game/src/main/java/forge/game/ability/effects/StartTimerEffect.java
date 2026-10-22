package forge.game.ability.effects;

import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Noyd, Keeper of the Void: "if you don't have a timer running, start a 30-minute timer" - real time (user ruling
 * 2026-10-21), checked when a player would receive priority (GameAction.checkRealTimeTimers), which then does what
 * the host's TimerExpires SVar says.
 */
public class StartTimerEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Start a " + sa.getParam("Minutes") + "-minute timer.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        if (p.hasTimerRunning()) {
            return;
        }
        final long minutes = Long.parseLong(sa.getParam("Minutes"));
        p.startTimer(minutes * 60_000_000_000L, sa.getHostCard());
        p.getGame().getGameLog().add(GameLogEntryType.INFORMATION, p + " starts a " + minutes + "-minute timer for "
                + sa.getHostCard() + ".");
    }
}
