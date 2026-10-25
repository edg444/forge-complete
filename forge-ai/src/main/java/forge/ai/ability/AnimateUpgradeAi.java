package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ Upgrade (Dragonshift, Dance of the Skywise): one of its creatures becomes a set creature (a 4/4 flying
 * Dragon) and loses its abilities until end of turn - worth it, before attacking, on a creature that's worth less than
 * what it becomes.
 */
public final class AnimateUpgradeAi {

    private AnimateUpgradeAi() { }

    // what becoming the set creature adds, on the same scale as evaluateCreature, less abilities it loses
    private static int gain(final SpellAbility sa, final Card c) {
        final int p = Integer.parseInt(sa.getParamOrDefault("Power", "0"));
        final int t = Integer.parseInt(sa.getParamOrDefault("Toughness", "0"));
        final boolean flying = sa.getParamOrDefault("Keywords", "").contains("Flying");
        int g = (p - c.getNetPower()) * 15 + (t - c.getNetToughness()) * 10;
        if (flying && !c.hasKeyword(forge.game.keyword.Keyword.FLYING)) {
            g += p * 10;
        }
        if (c.getSpellAbilities().anyMatch(SpellAbility::isActivatedAbility) || !c.getTriggers().isEmpty() || !c.getStaticAbilities().isEmpty()) {
            g -= 50;
        }
        return g;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1) || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getWeakestOpponent();
        sa.resetTargets();
        Card best = null;
        int gain = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().equals(ai) || opp == null || !CombatUtil.canAttack(c, opp)) {
                continue;
            }
            final int g = gain(sa, c);
            if (g > gain) {
                gain = g;
                best = c;
            }
        }
        if (best == null || gain < 60) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
