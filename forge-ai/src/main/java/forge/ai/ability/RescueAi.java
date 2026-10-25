package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * Saving its creatures, and a few one-combat effects.
 * <ul>
 * <li>MassBlink (Planar Guide: all creatures exiled until the end step): when their attack would kill it, or in answer
 * to their sweeper.</li>
 * <li>SaveBounce (Portal of Sanctuary, Scapegoat): its creatures back to hand in answer to what would destroy them.</li>
 * <li>BlockAny (Valor Made Real): before blocks, on its creature that can block two or more of their attackers and
 * live.</li>
 * <li>Outmaneuver (Outmaneuver): once blocks are known, the fewest blocked attackers whose damage as unblocked is
 * lethal.</li>
 * <li>CantBlockSelf (Duct Crawler): as it attacks, their best creature that could block it.</li>
 * </ul>
 */
public final class RescueAi {

    private RescueAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return "MassBlink".equals(logic) || "SaveBounce".equals(logic) || "BlockAny".equals(logic) || "Outmaneuver".equals(logic)
                || "CantBlockSelf".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "MassBlink" -> massBlink(ai, sa);
            case "SaveBounce" -> saveBounce(ai, sa);
            case "BlockAny" -> blockAny(ai, sa);
            case "Outmaneuver" -> outmaneuver(ai, sa);
            default -> cantBlockSelf(ai, sa);
        };
    }

    private static boolean sweeperComing(final Player ai) {
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s.getActivatingPlayer().isOpponentOf(ai) && (s.getApi() == ApiType.DestroyAll || s.getApi() == ApiType.DamageAll)) {
                return true;
            }
        }
        return false;
    }

    private static AiAbilityDecision massBlink(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (sweeperComing(ai) && !ai.getCreaturesInPlay().isEmpty()) {
            return YES;
        }
        if (combat != null && !combat.getAttackingPlayer().equals(ai) && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && ComputerUtilCombat.lifeInDanger(ai, combat)) {
            return YES;
        }
        return no(AiPlayDecision.AnotherTime);
    }

    private static AiAbilityDecision saveBounce(final Player ai, final SpellAbility sa) {
        if (ai.getGame().getStack().isEmpty()) {
            return no(AiPlayDecision.AnotherTime);
        }
        final List<GameObject> doomed = ComputerUtil.predictThreatenedObjects(ai, null);
        final List<Card> save = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai) && doomed.contains(c));
        // Scapegoat sacrifices one of them to save the rest
        final boolean sacrifices = sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(forge.game.cost.CostSacrifice.class);
        if (save.isEmpty() || sacrifices && save.size() < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        save.sort(java.util.Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        sa.resetTargets();
        final int max = sacrifices ? save.size() - 1 : 1;
        save.stream().limit(Math.min(max, sa.getMaxTargets())).forEach(c -> sa.getTargets().add(c));
        if (sacrifices) {
            // the one left behind is the cheapest, and the cost takes it
            SacrificePayoffAi.plan(sa.getHostCard(), save.get(save.size() - 1));
        }
        return YES;
    }

    private static AiAbilityDecision blockAny(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        Card best = null;
        int bestKills = 1;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().equals(ai) || c.isTapped()) {
                continue;
            }
            int power = 0;
            int kills = 0;
            for (final Card a : combat.getAttackersOf(ai)) {
                if (CombatUtil.canBlock(a, c)) {
                    power += a.getNetCombatDamage();
                    kills += c.getNetCombatDamage() >= a.getLethalDamage() ? 1 : 0;
                }
            }
            if (power < c.getNetToughness() && kills > bestKills) {
                best = c;
                bestKills = kills;
            }
        }
        if (best == null) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision outmaneuver(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        for (final Player opp : ai.getOpponents()) {
            final int need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            if (need <= 0 || !opp.canLoseLife()) {
                continue;
            }
            final List<Card> blocked = CardLists.filter(combat.getAttackers(), a -> combat.isBlocked(a) && opp.equals(combat.getDefenderPlayerByAttacker(a))
                    && sa.canTarget(a));
            blocked.sort(java.util.Comparator.comparingInt(Card::getNetCombatDamage).reversed());
            int dmg = 0;
            int n = 0;
            for (final Card a : blocked) {
                dmg += a.getNetCombatDamage();
                n++;
                if (dmg >= need) {
                    sa.resetTargets();
                    blocked.subList(0, n).forEach(c -> sa.getTargets().add(c));
                    sa.setXManaCostPaid(n);
                    return YES;
                }
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision cantBlockSelf(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final Card host = sa.getHostCard();
        if (combat == null || !combat.isAttacking(host) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(sa),
                c -> c.getController().isOpponentOf(ai) && c.isUntapped() && CombatUtil.canBlock(host, c)));
        if (best == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }
}
