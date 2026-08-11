package forge.game.cost;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

import java.util.Map;

/**
 * A literal sequence of tap and untap symbols, e.g. Snow Mercy's {T}, {Q}, {T}, {Q}, {T} - shaking
 * a snow globe.
 * <p>
 * Ordinary cost parts can't express this: {@link CostPart#paymentOrder()} sorts them (tap is -1,
 * untap 20), so writing "T Q T Q T" would pay every tap first and then every untap, and duplicate
 * parts don't survive as separate steps anyway. This keeps the sequence as one part and walks it in
 * order at payment time.
 * <p>
 * It extends {@link CostTap} so cost visitors keep seeing a tap cost, which is what it amounts to:
 * a sequence starting and ending on {T} needs this untapped and leaves it tapped.
 */
public class CostTapSequence extends CostTap {
    private static final long serialVersionUID = 1L;
    private static final long SHAKE_STEP_DELAY_MS = 220L;

    private final String steps;

    public CostTapSequence(final String steps) {
        this.steps = steps == null ? "T" : steps.toUpperCase();
    }

    public String getSteps() {
        return steps;
    }

    private boolean endsTapped() {
        return steps.charAt(steps.length() - 1) == 'T';
    }

    private boolean hasUntapStep() {
        return steps.indexOf('Q') >= 0;
    }

    @Override
    public boolean isUndoable() {
        return false;
    }

    @Override
    public boolean isReusable() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(steps.charAt(i) == 'Q' ? "{Q}" : "{T}");
        }
        return sb.toString();
    }

    @Override
    public void refund(final Card source) {
        source.setTapped(!endsTapped());
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        if (source.isAbilitySick()) {
            return false;
        }
        // the first symbol decides what state it has to start in; the rest have to be possible too
        final boolean startsTapped = steps.charAt(0) == 'Q';
        if (startsTapped ? !source.isTapped() : source.isTapped()) {
            return false;
        }
        if (steps.indexOf('T') >= 0 && !source.canTap()) {
            return false;
        }
        if (!hasUntapStep()) {
            return true;
        }
        // A stun counter is fatal here, unlike for a plain {Q} cost where removing the counter is
        // accepted as payment: the untap is replaced, so this stays tapped and the {T} step that
        // follows can no longer be paid - the sequence simply can't be completed.
        if (source.getCounters(CounterEnumType.STUN) > 0) {
            return false;
        }
        // predict mode: the untap steps come later in the sequence, so this asks whether untapping
        // will be allowed then, not whether it is tapped right now (it isn't - the sequence opens
        // with {T}, and canUntap(.., false) would refuse on that alone)
        return source.canUntap(null, true);
    }

    /**
     * Tapping and untapping fire card-tapped events the GUI animates, but the whole payment happens
     * in one go, so without a gap between steps only the final state is ever drawn - the shake would
     * be invisible. This is cosmetic only; nothing about the game state depends on the delay.
     */
    private static void pauseBetweenSteps() {
        try {
            Thread.sleep(SHAKE_STEP_DELAY_MS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision decision, final SpellAbility ability,
            final boolean effect) {
        final Card hostCard = ability.getHostCard();
        for (int i = 0; i < steps.length(); i++) {
            if (i > 0) {
                pauseBetweenSteps();
            }
            if (steps.charAt(i) == 'Q') {
                if (hostCard.untap()) {
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    final Map<Player, CardCollection> map = Maps.newHashMap();
                    map.put(payer, new CardCollection(hostCard));
                    runParams.put(AbilityKey.Map, map);
                    payer.getGame().getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
                }
            } else if (hostCard.tap(true, ability, payer)) {
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Cards, new CardCollection(hostCard));
                payer.getGame().getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
            }
        }
        return true;
    }
}
