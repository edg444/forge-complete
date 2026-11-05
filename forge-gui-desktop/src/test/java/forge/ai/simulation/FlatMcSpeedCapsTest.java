package forge.ai.simulation;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** The fork's speed caps on upstream's Flat Monte Carlo settings (see {@link FlatMcSpeedCaps}). */
public class FlatMcSpeedCapsTest {

    @Test
    public void phoneSettingsAreCappedAndSkipTheOpponentsTurn() {
        final FlatMonteCarloPicker.Config c = FlatMcSpeedCaps.apply(new FlatMonteCarloPicker.Config().mobileLite(), true);
        assertEquals(c.budgetNanos, TimeUnit.MILLISECONDS.toNanos(FlatMcSpeedCaps.PHONE_DECISION_MS));
        assertEquals(c.turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(FlatMcSpeedCaps.PHONE_TURN_MS));
        assertEquals(c.opponentLookaheadPercent, 0);
        assertFalse(c.landChoice);
    }

    @Test
    public void desktopSettingsAreCapped() {
        final FlatMonteCarloPicker.Config c = FlatMcSpeedCaps.apply(new FlatMonteCarloPicker.Config().maximized(), false);
        final long decision = TimeUnit.MILLISECONDS.toNanos(
                FlatMonteCarloPicker.constrainedHeap() ? FlatMcSpeedCaps.PHONE_DECISION_MS : FlatMcSpeedCaps.DESKTOP_DECISION_MS);
        assertEquals(c.budgetNanos, decision);
        assertFalse(c.landChoice);
    }

    @Test
    public void capsNeverRaiseASmallerBudget() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.budgetNanos = TimeUnit.MILLISECONDS.toNanos(10);
        c.turnBudgetNanos = TimeUnit.MILLISECONDS.toNanos(20);
        FlatMcSpeedCaps.apply(c, false);
        assertEquals(c.budgetNanos, TimeUnit.MILLISECONDS.toNanos(10));
        assertEquals(c.turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(20));
    }

    @Test
    public void capsStayBelowUpstreamsLimits() {
        assertTrue(FlatMcSpeedCaps.PHONE_DECISION_MS < FlatMonteCarloPicker.MOBILE_DECISION_MS);
        assertTrue(FlatMcSpeedCaps.PHONE_TURN_MS < FlatMonteCarloPicker.MOBILE_TURN_MS);
        assertTrue(FlatMcSpeedCaps.DESKTOP_DECISION_MS < FlatMonteCarloPicker.MAX_DECISION_MS_DESKTOP);
        assertTrue(FlatMcSpeedCaps.DESKTOP_TURN_MS < FlatMonteCarloPicker.MAX_TURN_MS_DESKTOP);
    }
}
