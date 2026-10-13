package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Problematic Volcano: the AI keeps its creatures where they can block the most opposing power. */
public class ChooseSideAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static boolean preferLeft(final Player ai, final Card volcano) {
        int left = 0, right = 0;
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCreaturesInPlay()) {
                final Boolean side = c.getVolcanoSide(volcano);
                if (side == null) {
                    continue;
                }
                if (side) {
                    left += Math.max(0, c.getNetCombatDamage());
                } else {
                    right += Math.max(0, c.getNetCombatDamage());
                }
            }
        }
        return left >= right;
    }
}
