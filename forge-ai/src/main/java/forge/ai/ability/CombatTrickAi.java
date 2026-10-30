package forge.ai.ability;

import java.util.List;

import forge.ai.AiTeams;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ CombatTrick: one-combat spells with no AI of their own.
 * <ul>
 * <li>Fog Patch (attacking creatures become blocked): on their attack, once blocks are known, when what's unblocked
 * would kill it or deal 4 or more.</li>
 * <li>Feint (tap the blockers of an attacker, and none of them deal combat damage): the fight where it would lose the
 * most of its own.</li>
 * <li>Angelic Favor (a 4/4 flying Angel for the combat): on their attack, before blocks, as a blocker that kills an
 * attacker and lives, or when their attack would kill it.</li>
 * </ul>
 */
public final class CombatTrickAi {

    private CombatTrickAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        if (combat == null) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final boolean theirAttack = !combat.getAttackingPlayer().equals(ai);
        if (sa.getApi() == ApiType.BecomesBlocked) {
            if (!theirAttack || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            final int unblocked = ComputerUtilCombat.sumDamageIfUnblocked(combat.getUnblockedAttackers(), ai);
            return ComputerUtilCombat.lifeInDanger(ai, combat) || unblocked >= 4 ? YES : no(AiPlayDecision.DoesntImpactCombat);
        }
        if (sa.getApi() == ApiType.Token) {
            if (!theirAttack || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            // the Angel is a 4/4 flier
            for (final Card a : combat.getAttackersOf(ai)) {
                if (a.getNetToughness() <= 4 && a.getNetCombatDamage() < 4 && ComputerUtilCard.evaluateCreature(a) >= 120) {
                    return YES;
                }
            }
            return ComputerUtilCombat.lifeInDanger(ai, combat) ? YES : no(AiPlayDecision.DoesntImpactCombat);
        }
        if (sa.getApi() == ApiType.TapAll && sa.usesTargeting()) {
            if (!ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            Card best = null;
            int bestValue = 0;
            for (final Card a : CardUtil.getValidCardsToTarget(sa)) {
                final List<Card> blockers = combat.getBlockers(a);
                if (blockers.isEmpty()) {
                    continue;
                }
                // no combat damage either way in this fight: what each side would have lost
                int v = 0;
                if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, a, combat)) {
                    v -= AiTeams.sign(ai, a.getController()) * ComputerUtilCard.evaluateCreature(a);
                }
                for (final Card b : blockers) {
                    if (ComputerUtilCombat.blockerWouldBeDestroyed(ai, b, combat)) {
                        v -= AiTeams.sign(ai, b.getController()) * ComputerUtilCard.evaluateCreature(b);
                    }
                }
                if (v > bestValue) {
                    bestValue = v;
                    best = a;
                }
            }
            if (best == null || bestValue < 100) {
                return no(AiPlayDecision.DoesntImpactCombat);
            }
            sa.resetTargets();
            sa.getTargets().add(best);
            return YES;
        }
        return no(AiPlayDecision.CantPlayAi);
    }
}
