package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Four-faced: "move it to the next stage, or to stage 1 if it's at stage 4" - if it's still the same permanent. */
public class AdvanceStageEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Card now = game.getCardState(host, null);
        if (now == null || !now.isInPlay() || !now.equalsWithGameTimestamp(host)) {
            return;
        }
        now.advanceStage();
        game.getAction().checkStaticAbilities();
        // a stage's abilities may bring triggers (evolve) with them
        game.getTriggerHandler().clearActiveTriggers(now, null);
        game.getTriggerHandler().registerActiveTrigger(now, false);
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard() + " moves to its next stage.";
    }
}
