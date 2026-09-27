package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.NudgeNumberEffect;
import forge.game.card.Card;
import forge.game.card.NumberInstances;
import forge.game.card.NumberNudge;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * More or Less. The AI casts it to kill an opposing creature one point of toughness would kill. When choosing,
 * it lowers numbers on opponents' objects and raises them on its own - toughness first on an opponent's
 * creature, power first on its own.
 */
public class NudgeNumberAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        Card best = null;
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCreaturesInPlay()) {
                if (!sa.canTarget(c) || !hasPrintedToughness(c) || !diesToOneLessToughness(c)) {
                    continue;
                }
                if (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best)) {
                    best = c;
                }
            }
        }
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean hasPrintedToughness(final Card c) {
        for (final NumberNudge n : NumberInstances.of(c)) {
            if (n.kind == NumberNudge.Kind.TOUGHNESS) {
                return true;
            }
        }
        return false;
    }

    private static boolean diesToOneLessToughness(final Card c) {
        final int left = c.getNetToughness() - c.getDamage();
        return c.getNetToughness() <= 1 || (left <= 1 && !c.hasKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Override
    public String chooseString(final Player ai, final SpellAbility sa, final List<String> options) {
        // a spell on the stack is targeted as its spell ability
        Card tgt = sa.getTargetCard();
        if (tgt == null && sa.getTargets().getFirstTargetedSpell() != null) {
            tgt = sa.getTargets().getFirstTargetedSpell().getHostCard();
        }
        final boolean theirs = tgt != null && tgt.getController().isOpponentOf(ai);
        if (!options.isEmpty() && (options.get(0).startsWith(NudgeNumberEffect.ADD)
                || options.get(0).startsWith(NudgeNumberEffect.SUBTRACT))) {
            final String want = theirs ? NudgeNumberEffect.SUBTRACT : NudgeNumberEffect.ADD;
            for (final String o : options) {
                if (o.startsWith(want)) {
                    return o;
                }
            }
            return options.get(0);
        }
        final String prefer = theirs ? "Toughness" : "Power";
        for (final String o : options) {
            if (o.startsWith(prefer)) {
                return o;
            }
        }
        return options.isEmpty() ? null : options.get(0);
    }
}
