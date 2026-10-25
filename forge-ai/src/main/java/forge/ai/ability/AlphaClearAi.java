package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Spells that take their blockers out of the way for one attack.
 * <ul>
 * <li>Sudden Disappearance (their nonland permanents exiled until the end step), Goblin War Cry and Eunuchs' Intrigues
 * (all but one of their creatures can't block): in its first main phase, when the attack is lethal with the blockers
 * that are left (the one they keep blocks its biggest attacker) and isn't without the spell.</li>
 * <li>Sudden Spoiling (their creatures become 0/2 with no abilities): once blocks are known, when its creatures in
 * the fight would die and won't, or on their turn when their attack would kill it.</li>
 * </ul>
 */
public final class AlphaClearAi {

    private AlphaClearAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        // the BestBlocker logic is the opponent's choice when it resolves; casting it is this
        return "AlphaClear".equals(logic) || "BestBlocker".equals(logic) && sa.getApi() == ApiType.ChooseCard && sa.usesTargeting();
    }

    private static int damage(final List<Integer> powers, final int blockers) {
        return powers.stream().skip(Math.max(0, blockers)).mapToInt(Integer::intValue).sum();
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        if (sa.getApi() == ApiType.AnimateAll) {
            return spoil(ai, sa);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final List<Integer> powers = new ArrayList<>();
        for (final Card c : ai.getCreaturesInPlay()) {
            if (CombatUtil.canAttack(c)) {
                powers.add(Math.max(0, c.getNetCombatDamage()));
            }
        }
        powers.sort(Comparator.reverseOrder());
        // Goblin War Cry: one of theirs can still block
        final int left = sa.getApi() == ApiType.ChooseCard ? 1 : 0;
        for (final Player opp : ai.getOpponents()) {
            if (!sa.canTarget(opp) || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
                continue;
            }
            final int blockers = CardLists.count(opp.getCreaturesInPlay(), Card::isUntapped);
            if (blockers <= left || damage(powers, blockers) >= opp.getLife()) {
                // nothing for the spell to do, or lethal anyway
                continue;
            }
            if (damage(powers, left) >= opp.getLife()) {
                sa.resetTargets();
                sa.getTargets().add(opp);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision spoil(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Player attacker = combat.getAttackingPlayer();
        if (!attacker.equals(ai)) {
            // their attackers deal no damage
            if (!ComputerUtilCombat.lifeInDanger(ai, combat) || !sa.canTarget(attacker)) {
                return no(AiPlayDecision.DoesntImpactCombat);
            }
            sa.resetTargets();
            sa.getTargets().add(attacker);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // its blocked attackers live, and the 0/2 blockers die to 2 power
        int value = 0;
        Player opp = null;
        for (final Card a : combat.getAttackers()) {
            if (!a.getController().equals(ai) || combat.getBlockers(a).isEmpty()) {
                continue;
            }
            opp = combat.getBlockers(a).get(0).getController();
            if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, a, combat)) {
                value += ComputerUtilCard.evaluateCreature(a);
            }
            if (combat.getBlockers(a).size() == 1 && a.getNetCombatDamage() >= 2) {
                final Card b = combat.getBlockers(a).get(0);
                if (!ComputerUtilCombat.blockerWouldBeDestroyed(ai, b, combat)) {
                    value += ComputerUtilCard.evaluateCreature(b);
                }
            }
        }
        if (opp == null || value < 150 || !sa.canTarget(opp)) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
