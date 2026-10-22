package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Mana Max, Afterburner: "Whenever a player loses one or more unspent mana" - a mana pool emptying with mana still in
 * it at the end of a step or phase. ValidPlayer$ is that player.
 */
public class TriggerManaLost extends Trigger {

    public TriggerManaLost(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(final Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player));
    }

    @Override
    public void setTriggeringObjects(final SpellAbility sa, final Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(final SpellAbility sa) {
        return "Player: " + sa.getTriggeringObject(AbilityKey.Player);
    }
}
