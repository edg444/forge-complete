package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Form of the Approach of the Second Sun: one or more cards fell off a ValidPlayer$'s head (see HeadBalance). */
public class TriggerCardsFellOffHead extends Trigger {

    public TriggerCardsFellOffHead(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player));
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, final Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(final SpellAbility sa) {
        return "";
    }
}
