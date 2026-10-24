package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ EndCombat (Mandate of Peace): the opponent's combat ends as a Fog - once blocks are known, when the AI
 * stands to lose more there (its blockers dying, the damage it takes, lethal above all) than it gains (their
 * attackers dying).
 */
public final class EndCombatAi {

    private EndCombatAi() { }

    // Moment of Silence: before their combat, against an attack that could be lethal
    private static AiAbilityDecision skipCombat(final Player ai, final SpellAbility sa) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Player active = ph.getPlayerTurn();
        if (!active.isOpponentOf(ai) || !ph.is(PhaseType.MAIN1) || !sa.usesTargeting() || !sa.canTarget(active)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        int coming = 0;
        for (final Card c : active.getCreaturesInPlay()) {
            if (forge.game.combat.CombatUtil.canAttack(c, ai)) {
                coming += ComputerUtilCombat.damageIfUnblocked(c, ai, null, false);
            }
        }
        if (coming < ai.getLife() || !ai.canLoseLife()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(active);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        if (sa.getApi() == forge.game.ability.ApiType.SkipPhase) {
            return skipCombat(ai, sa);
        }
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || ai.equals(combat.getAttackingPlayer()) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        int lost = 0;
        int gained = 0;
        int damage = 0;
        for (final Card att : combat.getAttackers()) {
            if (combat.isUnblocked(att)) {
                damage += ComputerUtilCombat.damageIfUnblocked(att, ai, combat, false);
                continue;
            }
            for (final Card b : combat.getBlockers(att)) {
                if (ComputerUtilCombat.canDestroyBlocker(ai, b, att, combat, false)) {
                    lost += ComputerUtilCard.evaluateCreature(b);
                }
                if (ComputerUtilCombat.canDestroyAttacker(ai, att, b, combat, false)) {
                    gained += ComputerUtilCard.evaluateCreature(att);
                }
            }
        }
        if (damage >= ai.getLife() && ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife()) {
            lost += 10000;
        } else {
            lost += damage * (ComputerUtilCombat.lifeInDanger(ai, combat) ? 30 : 6);
        }
        return lost - gained >= 150 ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
