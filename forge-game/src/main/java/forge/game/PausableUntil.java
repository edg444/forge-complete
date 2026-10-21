package forge.game;

import forge.GameCommand;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * An "until end of turn" or "this turn" effect, named so Grand Marshal Macie can choose it. While a tapped Macie holds
 * it, it doesn't end: at its scheduled end it waits, and it ends as soon as she untaps or leaves the battlefield. If she
 * untaps or leaves before then, it just goes on to its usual end (rulings).
 */
public class PausableUntil implements GameCommand {
    private static final long serialVersionUID = 1L;

    private final transient Game game;
    private final String description;
    private final Player controller;
    private final boolean curse;
    private final GameCommand end;
    private transient Card holder = null;
    private boolean due = false;
    private boolean done = false;

    public PausableUntil(final Game game, final SpellAbility sa, final GameCommand end) {
        this.game = game;
        this.end = end;
        this.controller = sa.getActivatingPlayer();
        this.curse = sa.isCurse();
        String text = sa.getStackDescription();
        if (text == null || text.isBlank()) {
            text = sa.getDescription();
        }
        this.description = sa.getHostCard().getName() + ": " + text;
    }

    public String getDescription() {
        return description;
    }

    public Player getController() {
        return controller;
    }

    public boolean isCurse() {
        return curse;
    }

    public boolean isHeldBy(final Card c) {
        return holder != null && holder.equals(c);
    }

    /** Macie keeps this from ending for as long as she remains tapped. */
    public void hold(final Card macie) {
        holder = macie;
        macie.addUntapCommand(this::release);
        macie.addLeavesPlayCommand(this::release);
    }

    private boolean held() {
        return holder != null && holder.isInPlay() && holder.isTapped() && game.getCardState(holder) == holder;
    }

    private void release() {
        holder = null;
        if (due) {
            finish();
        }
    }

    @Override
    public void run() {
        if (done) {
            return;
        }
        if (held()) {
            due = true;
            return;
        }
        finish();
    }

    private void finish() {
        if (done) {
            return;
        }
        done = true;
        game.removePausableUntil(this);
        end.run();
    }
}
