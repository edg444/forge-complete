package forge.ai.ability;

import java.util.TreeSet;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ Shoal (the Betrayers of Kamigawa Shoals): X is paid in mana, or is the mana value of the card exiled for
 * the alternative cost, so the Xs on offer are what the mana covers or the mana values of the cards it could exile;
 * each Shoal takes the smallest of them that does its job.
 */
public final class ShoalAi {

    private ShoalAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static CostExile exileForX(final SpellAbility sa) {
        if (sa.getPayCosts() == null) {
            return null;
        }
        for (final CostPart p : sa.getPayCosts().getCostParts()) {
            if (p instanceof CostExile ce && ce.getType().contains("EQX")) {
                return ce;
            }
        }
        return null;
    }

    private static TreeSet<Integer> choices(final Player ai, final SpellAbility sa) {
        final TreeSet<Integer> xs = new TreeSet<>();
        final CostExile ce = exileForX(sa);
        if (ce != null) {
            // the type reads X, so each card is checked with X set to its own mana value
            final Card host = sa.getHostCard();
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                sa.setXManaCostPaid(c.getCMC());
                if (!c.equals(host) && c.isValid(ce.getType().split(";"), ai, host, sa)) {
                    xs.add(c.getCMC());
                }
            }
        } else if (sa.costHasManaX()) {
            sa.setXManaCostPaid(null);
            final int max = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
            for (int x = 0; x <= max; x++) {
                xs.add(x);
            }
        }
        sa.setXManaCostPaid(null);
        return xs;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final TreeSet<Integer> xs = choices(ai, sa);
        if (xs.isEmpty()) {
            return no(AiPlayDecision.CantAfford);
        }
        final ApiType api = sa.getApi();
        if (api == ApiType.Pump && sa.getParamOrDefault("NumDef", "").equals("-X")) {
            return shrink(ai, sa, xs);
        }
        if (api == ApiType.Pump && sa.getParamOrDefault("NumAtt", "").equals("+X")) {
            return lethalPump(ai, sa, xs);
        }
        if (api == ApiType.Pump && sa.getSubAbility() != null && sa.getSubAbility().getApi() == ApiType.Counter) {
            return counter(ai, sa, xs);
        }
        if (api == ApiType.GainLife) {
            return gainLife(ai, sa, xs);
        }
        if (api == ApiType.ChooseSource) {
            return redirect(ai, sa, xs);
        }
        return no(AiPlayDecision.CantPlayAi);
    }

    // Sickening Shoal: their best creature that some X kills, at the lowest such X
    private static AiAbilityDecision shrink(final Player ai, final SpellAbility sa, final TreeSet<Integer> xs) {
        Card best = null;
        int bestX = -1;
        for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
            if (!sa.canTarget(c) || ComputerUtilCard.evaluateCreature(c) < 120) {
                continue;
            }
            final Integer x = xs.ceiling(Math.max(1, c.getNetToughness() - c.getDamage()));
            if (x != null && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
                bestX = x;
            }
        }
        if (best == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        sa.setXManaCostPaid(bestX);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Blazing Shoal: an unblocked attacker made big enough to finish them, in life or in poison
    private static AiAbilityDecision lethalPump(final Player ai, final SpellAbility sa, final TreeSet<Integer> xs) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().isPlayerTurn(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        for (final Card a : combat.getUnblockedAttackers()) {
            final Player opp = combat.getDefenderPlayerByAttacker(a);
            if (opp == null || !opp.isOpponentOf(ai) || !sa.canTarget(a)) {
                continue;
            }
            final int need;
            if (a.hasKeyword(Keyword.INFECT)) {
                need = 10 - ComputerUtilCombat.resultingPoison(opp, combat);
            } else if (opp.canLoseLife()) {
                need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            } else {
                continue;
            }
            final Integer x = need > 0 ? xs.ceiling(need) : null;
            if (x != null) {
                sa.resetTargets();
                sa.getTargets().add(a);
                sa.setXManaCostPaid(x);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    // Disrupting Shoal: their spell on top of the stack, when an X matches its mana value
    private static AiAbilityDecision counter(final Player ai, final SpellAbility sa, final TreeSet<Integer> xs) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return no(AiPlayDecision.AnotherTime);
        }
        final SpellAbility top = game.getStack().peekAbility();
        if (!top.isSpell() || !top.getActivatingPlayer().isOpponentOf(ai) || !top.isCounterableBy(sa)
                || !sa.canTargetSpellAbility(top)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int mv = top.getHostCard().getCMC();
        // a land-cheap spell isn't worth the card
        if (!xs.contains(mv) || mv < 2 && exileForX(sa) != null) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.resetTargets();
        sa.getTargets().add(top);
        sa.setXManaCostPaid(mv);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Nourishing Shoal: just enough life to live through their attack, or with mana to spare at their end step
    private static AiAbilityDecision gainLife(final Player ai, final SpellAbility sa, final TreeSet<Integer> xs) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (!ai.canGainLife()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        if (combat != null && !game.getPhaseHandler().isPlayerTurn(ai) && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            final int left = ComputerUtilCombat.lifeThatWouldRemain(ai, combat);
            if (left <= 0 && ComputerUtilCombat.resultingPoison(ai, combat) < 10) {
                final Integer x = xs.ceiling(1 - left);
                if (x != null) {
                    sa.setXManaCostPaid(x);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return no(AiPlayDecision.AnotherTime);
        }
        if (exileForX(sa) == null && xs.last() >= 4 && game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getNextTurn().equals(ai)) {
            sa.setXManaCostPaid(xs.last());
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return no(AiPlayDecision.AnotherTime);
    }

    // Shining Shoal: the redirect AI judges each X; the smallest that moves the whole of the biggest hit, else the
    // biggest that's worth it
    private static AiAbilityDecision redirect(final Player ai, final SpellAbility sa, final TreeSet<Integer> xs) {
        final int biggest = RedirectCombatAi.biggestHit(ai);
        Integer chosen = null;
        for (final int x : xs.descendingSet()) {
            if (x <= 0) {
                break;
            }
            sa.setXManaCostPaid(x);
            for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
                sub.resetTargets();
            }
            if (!RedirectCombatAi.decide(ai, sa).willingToPlay()) {
                break;
            }
            if (chosen == null || x >= biggest) {
                chosen = x;
            }
        }
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            sub.resetTargets();
        }
        if (chosen == null) {
            sa.setXManaCostPaid(null);
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.setXManaCostPaid(chosen);
        return RedirectCombatAi.decide(ai, sa);
    }
}
