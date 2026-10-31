package forge.game.judgetower;

import forge.game.player.Player;

/**
 * A rules infraction caught mid-action. It unwinds the game thread to PhaseHandler, wherever the player was (casting,
 * resolving, declaring), which puts the game back at the last legal state and rules on it. An Error, because
 * effects and cost code catch Exception around their parsing and must not swallow it.
 */
public final class JudgeInfraction extends Error {
    private static final long serialVersionUID = 1L;

    private final transient Player offender;
    private final String key;
    private final boolean appealed;

    JudgeInfraction(final Player offender0, final String key0, final String reason) {
        this(offender0, key0, reason, false);
    }

    /** @param appealed0 they've been asked already and accepted it, there being something to show them first */
    public JudgeInfraction(final Player offender0, final String key0, final String reason, final boolean appealed0) {
        super(reason, null, false, false);
        offender = offender0;
        key = key0;
        appealed = appealed0;
    }

    public boolean isAppealed() {
        return appealed;
    }

    public Player getOffender() {
        return offender;
    }

    /** Identifies this ruling, so that one the player has had overruled isn't made again. */
    public String getKey() {
        return key;
    }
}
