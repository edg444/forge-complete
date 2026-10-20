package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Unstable's augment: a host has been combined with a card with augment (run by AugmentEffect, like Mutates). */
public class TriggerAugments extends Trigger {

    public TriggerAugments(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidCard", runParams.get(AbilityKey.Card))
                && matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player));
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return "Augmented: " + sa.getTriggeringObject(AbilityKey.Card);
    }
}
