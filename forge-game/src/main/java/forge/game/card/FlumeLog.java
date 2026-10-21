package forge.game.card;

import forge.game.player.Player;

/**
 * Log Flume: creatures that jumped in a log together. They can't be blocked unless they're all blocked, and a spell or
 * ability the log's player controls that targets one of them targets all of them (CombatUtil, MagicStack).
 */
public final class FlumeLog {
    private final Player owner;
    private final CardCollection members;

    public FlumeLog(final Player owner, final CardCollection members) {
        this.owner = owner;
        this.members = members;
    }

    public Player getOwner() {
        return owner;
    }

    /** The creatures still in this log: on the battlefield, and not out of it since. */
    public CardCollection getMembers() {
        return CardLists.filter(members, c -> c.isInPlay() && c.getFlumeLog() == this);
    }
}
