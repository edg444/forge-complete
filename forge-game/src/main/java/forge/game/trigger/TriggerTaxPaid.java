package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Tax Taker: a player (ValidPayer$) paid mana extra because of a spell or permanent another player controls
 * (ValidTaxer$): an "unless they pay" cost, an attack or block cost (all of one declaration's at once), or a cost
 * increase (user ruling 2026-10-22). Non-mana taxes don't count (rulings).
 */
public class TriggerTaxPaid extends Trigger {

    public TriggerTaxPaid(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidPayer", runParams.get(AbilityKey.Player))) {
            return false;
        }
        if (!matchesValidParam("ValidTaxer", runParams.get(AbilityKey.Taxer))) {
            return false;
        }
        return true;
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, final Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Amount, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(final SpellAbility sa) {
        return "Payer: " + sa.getTriggeringObject(AbilityKey.Player) + ", Amount: " + sa.getTriggeringObject(AbilityKey.Amount);
    }
}
