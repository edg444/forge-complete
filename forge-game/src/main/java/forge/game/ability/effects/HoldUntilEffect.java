package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.PausableUntil;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Grand Marshal Macie: choose an "until end of turn" or "this turn" effect, from any source; as long as she remains
 * tapped, it doesn't end (PausableUntil). If she has already untapped as this resolves, no effect is chosen (rulings).
 */
public class HoldUntilEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Choose an \"until end of turn\" or \"this turn\" effect. As long as " + sa.getHostCard()
                + " remains tapped, that effect doesn't end.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Card macie = host.getGame().getCardState(host);
        if (macie == null || !macie.isInPlay() || !macie.isTapped()) {
            return;
        }
        final List<PausableUntil> effects = Lists.newArrayList(host.getGame().getPausableUntils());
        if (effects.isEmpty()) {
            return;
        }
        final Map<String, PausableUntil> byName = Maps.newLinkedHashMap();
        for (final PausableUntil e : effects) {
            String name = e.getDescription();
            for (int n = 2; byName.containsKey(name); n++) {
                name = e.getDescription() + " (" + n + ")";
            }
            byName.put(name, e);
        }
        final String chosen = you.getController().isAI() ? aiChoice(you, byName)
                : you.getController().chooseSomeType("effect", sa, Lists.newArrayList(byName.keySet()));
        if (chosen == null || !byName.containsKey(chosen)) {
            return;
        }
        byName.get(chosen).hold(macie);
        host.getGame().getAction().notifyOfValue(sa, you, chosen, null);
    }

    /** The AI keeps its own most recent effect that isn't a curse, if it has one. */
    private static String aiChoice(final Player ai, final Map<String, PausableUntil> byName) {
        String best = null;
        for (final Map.Entry<String, PausableUntil> e : byName.entrySet()) {
            if (ai.equals(e.getValue().getController()) && !e.getValue().isCurse()) {
                best = e.getKey();
            }
        }
        return best;
    }
}
