package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Motivate: before combat when the creature could attack this turn, otherwise after it so it can block. */
public class MotivateAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final boolean main1 = ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai);
        if (main1 && host.isSick()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
