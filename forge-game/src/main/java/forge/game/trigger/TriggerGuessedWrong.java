package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * The Commander Recommender: "Whenever an opponent guesses wrong" - run by the effects where a player guesses (see
 * GuessedWrongEffect.fire), with the guesser as ValidPlayer$.
 */
public class TriggerGuessedWrong extends Trigger {

    public TriggerGuessedWrong(final Map<String, String> params, final Card host, final boolean intrinsic) {
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
        return "Guesser: " + sa.getTriggeringObject(AbilityKey.Player);
    }
}
