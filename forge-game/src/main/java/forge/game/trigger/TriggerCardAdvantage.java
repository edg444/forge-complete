package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Toofer, Keeper of the Full Grip: a spell its controller (ValidPlayer$) controls resolved and left them with card
 * advantage (MagicStack).
 */
public class TriggerCardAdvantage extends Trigger {

    public TriggerCardAdvantage(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))
                && matchesValidParam("ValidCard", runParams.get(AbilityKey.Card));
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, final Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Player, AbilityKey.Card);
    }

    @Override
    public String getImportantStackObjects(final SpellAbility sa) {
        return "Spell: " + sa.getTriggeringObject(AbilityKey.Card);
    }
}
