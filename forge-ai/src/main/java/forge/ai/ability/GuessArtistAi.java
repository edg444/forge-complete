package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;

public class GuessArtistAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting() && !doTgt(ai, sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting() && !doTgt(ai, sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private boolean doTgt(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final PlayerCollection opps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        if (opps.isEmpty()) {
            return false;
        }
        sa.getTargets().add(opps.getFirst());
        return true;
    }
}
