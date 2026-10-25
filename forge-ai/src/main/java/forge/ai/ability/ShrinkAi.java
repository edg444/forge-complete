package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * <ul>
 * <li>AILogic$ Shrink (Turn to Frog, Snakeform): their creature becomes a 1/1 with no abilities, once blocks are known -
 * worth the creature if what it's fighting now kills it, and the AI's creatures it would have killed, or the damage it
 * would have done unblocked.</li>
 * <li>AILogic$ StripAttachments (Strip Bare): every Aura and Equipment on one creature - theirs helping their creature,
 * or theirs holding down its own.</li>
 * </ul>
 */
public final class ShrinkAi {

    private ShrinkAi() { }

    public static boolean handles(final String logic) {
        return "Shrink".equals(logic) || "StripAttachments".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return "Shrink".equals(sa.getParam("AILogic")) ? shrink(ai, sa) : strip(ai, sa);
    }

    private static AiAbilityDecision shrink(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        sa.resetTargets();
        // Polymorphist's Jest: every creature the player controls
        if (sa.getTargetRestrictions().canTgtPlayer()) {
            int total = 0;
            Player who = null;
            for (final Player p : ai.getOpponents()) {
                if (!sa.canTarget(p)) {
                    continue;
                }
                int v = 0;
                for (final Card c : p.getCreaturesInPlay()) {
                    v += shrinkValue(ai, combat, c);
                }
                if (v > total) {
                    total = v;
                    who = p;
                }
            }
            if (who == null || total < 150) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.getTargets().add(who);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        Card best = null;
        int bestV = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().isOpponentOf(ai)) {
                continue;
            }
            final int v = shrinkValue(ai, combat, c);
            if (v > bestV) {
                bestV = v;
                best = c;
            }
        }
        if (best == null || bestV < 100) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static int shrinkValue(final Player ai, final Combat combat, final Card c) {
        int v = 0;
        if (combat.isAttacking(c)) {
            if (combat.isUnblocked(c)) {
                v += Math.max(0, ComputerUtilCombat.damageIfUnblocked(c, ai, combat, false) - 1) * 6;
            }
            for (final Card b : combat.getBlockers(c)) {
                if (b.getNetCombatDamage() >= 1) {
                    v += ComputerUtilCard.evaluateCreature(c);
                }
                if (ComputerUtilCombat.canDestroyBlocker(ai, b, c, combat, false) && b.getNetToughness() > 1) {
                    v += ComputerUtilCard.evaluateCreature(b);
                }
            }
        } else if (combat.isBlocking(c)) {
            for (final Card a : combat.getAttackersBlockedBy(c)) {
                if (a.getNetCombatDamage() >= 1) {
                    v += ComputerUtilCard.evaluateCreature(c);
                }
                if (ComputerUtilCombat.canDestroyAttacker(ai, a, c, combat, false) && a.getNetToughness() > 1) {
                    v += ComputerUtilCard.evaluateCreature(a);
                }
            }
        }
        return v;
    }

    private static AiAbilityDecision strip(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        Card best = null;
        int bestV = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            int v = 0;
            for (final Card a : c.getAttachedCards()) {
                final int w = (a.getCMC() + 1) * 25;
                // theirs on anything is a card of theirs gone; its own a card of its own
                v += a.getController().isOpponentOf(ai) ? w : -w;
                // and what it did: helping their creature, or holding down its own
                if (a.getController().isOpponentOf(ai) && c.getController().equals(ai)) {
                    v += ComputerUtilCard.evaluateCreature(c) / 3;
                }
            }
            if (v > bestV) {
                bestV = v;
                best = c;
            }
        }
        if (best == null || bestV < 60) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
