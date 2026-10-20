package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Boomstacker: "When the stack falls, ..." - run by Card when its simulated dice stack topples (DiceStack). */
public class TriggerDiceStackFell extends Trigger {

    public TriggerDiceStackFell(Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidCard", runParams.get(AbilityKey.Card));
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return "";
    }
}
