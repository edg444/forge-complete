package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;

/**
 * Trigger Happy: one of the target permanent's triggered abilities triggers, and its caster controls it. Only abilities
 * that start "When", "Whenever" or "At" (keyword ones included) - not reflexive or delayed ones, which aren't the
 * permanent's own triggers here. The trigger event is ignored; an intervening "if" is still checked as it resolves, and
 * the caster chooses the targets as it's put on the stack (rulings).
 */
public class TriggerAbilityEffect extends SpellAbilityEffect {

    /** The triggered abilities of a permanent that can be made to trigger: its visible, non-static ones. */
    public static List<Trigger> triggersOf(final Card c) {
        final List<Trigger> result = Lists.newArrayList();
        for (final Trigger t : c.getTriggers()) {
            if (!t.isStatic() && !t.isSecondary() && t.ensureAbility() != null) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "A triggered ability of " + getTargetCards(sa) + " triggers.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        for (final Card target : getTargetCards(sa)) {
            final Card c = game.getCardState(target);
            if (c == null || !c.isInPlay()) {
                continue;
            }
            final List<Trigger> triggers = triggersOf(c);
            if (triggers.isEmpty()) {
                continue;
            }
            final Map<SpellAbility, Trigger> byAbility = Maps.newLinkedHashMap();
            for (final Trigger t : triggers) {
                byAbility.put(t.ensureAbility(), t);
            }
            final SpellAbility picked = you.getController().chooseSingleSpellForEffect(Lists.newArrayList(byAbility.keySet()),
                    sa, "Choose a triggered ability of " + c + " to trigger", null);
            final Trigger chosen = picked == null ? triggers.get(0) : byAbility.get(picked);
            final SpellAbility ability = chosen.ensureAbility().copy(c, you, false);
            ability.setActivatingPlayer(you);
            final WrappedAbility wrapper = new WrappedAbility(chosen, ability, you);
            wrapper.setActivatingPlayer(you);
            game.getStack().addSimultaneousStackEntry(wrapper);
        }
    }
}
