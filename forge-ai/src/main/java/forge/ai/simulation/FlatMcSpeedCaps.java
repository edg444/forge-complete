package forge.ai.simulation;

import java.util.concurrent.TimeUnit;

/**
 * The fork's speed limits for Flat Monte Carlo, applied on top of upstream's strength settings (mobileLite and
 * maximized raise the search, never lower it). Kept out of FlatMonteCarloPicker so upstream's daily edits there merge
 * cleanly.
 *
 * Measured 2026-11-04 on desktop, seeded AI-vs-AI games: Flat MC made games 2-2.5x slower, a rollout cost ~110 ms
 * even on a small board (copying the whole game is ~40% of it), and the land-drop comparison alone was ~24% of all
 * samples while changing the land in none of its decisions. On a phone every rollout also churns a small heap, so the
 * garbage collector slows down the rest of the game too.
 */
public final class FlatMcSpeedCaps {
    static final int PHONE_DECISION_MS = 250;
    static final int PHONE_TURN_MS = 1000;
    static final int DESKTOP_DECISION_MS = 750;
    static final int DESKTOP_TURN_MS = 3000;
    /** -Dflatmc.nocaps=true runs upstream's settings unchanged, to measure what the caps save. */
    private static final boolean NO_CAPS = Boolean.getBoolean("flatmc.nocaps");

    private FlatMcSpeedCaps() {
    }

    /**
     * Caps the time a configuration may spend. The phone (or a desktop with a phone-sized heap, which upstream already
     * gives the phone settings) also skips playing out the opponent's turn, the most expensive part of a rollout.
     */
    public static FlatMonteCarloPicker.Config apply(final FlatMonteCarloPicker.Config cfg, final boolean phone) {
        if (NO_CAPS) {
            return cfg;
        }
        final boolean small = phone || FlatMonteCarloPicker.constrainedHeap();
        cfg.landChoice = false;
        cfg.budgetNanos = Math.min(cfg.budgetNanos, TimeUnit.MILLISECONDS.toNanos(small ? PHONE_DECISION_MS : DESKTOP_DECISION_MS));
        cfg.turnBudgetNanos = Math.min(cfg.turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(small ? PHONE_TURN_MS : DESKTOP_TURN_MS));
        if (small) {
            cfg.opponentLookaheadPercent = 0;
        }
        return cfg;
    }
}
