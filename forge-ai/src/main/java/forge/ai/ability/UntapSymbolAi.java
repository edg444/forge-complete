package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostUntap;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ Untap (the Shadowmoor {Q} creatures): the untap is most of the value, so the ability goes once the creature
 * is tapped and the AI's own combat is over (it can block again), or on their turn before blocks; the effect itself
 * only needs a harmless use. Curses go on their best creature, a help on the creature itself, a player target is an
 * opponent, and Leech Bonder moves a -1/-1 counter off its own side onto theirs (or a +1/+1 counter the other way).
 */
public final class UntapSymbolAi {

    private UntapSymbolAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostUntap.class) || !host.isTapped()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final boolean afterOwnCombat = ph.isPlayerTurn(ai) && ph.getPhase().isAfter(PhaseType.COMBAT_END);
        final boolean beforeTheirBlocks = !ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS);
        if (!afterOwnCombat && !beforeTheirBlocks) {
            return no(AiPlayDecision.AnotherTime);
        }
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            sub.resetTargets();
            if (!sub.usesTargeting()) {
                continue;
            }
            if (sub.getApi() == ApiType.MoveCounter && !moveCounter(ai, sub)) {
                return no(AiPlayDecision.TargetingFailed);
            }
            if (sub.getApi() != ApiType.MoveCounter && !target(ai, sub)) {
                return no(AiPlayDecision.TargetingFailed);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean target(final Player ai, final SpellAbility sa) {
        if (sa.isCurse()) {
            final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getOpponents().getCreaturesInPlay(), sa::canTarget));
            if (best != null) {
                sa.getTargets().add(best);
                return true;
            }
            return false;
        }
        if (sa.canTarget(sa.getHostCard())) {
            sa.getTargets().add(sa.getHostCard());
            return true;
        }
        for (final Player p : ai.getOpponents()) {
            if (sa.canTarget(p)) {
                sa.getTargets().add(p);
                return true;
            }
        }
        if (sa.canTarget(ai)) {
            sa.getTargets().add(ai);
            return true;
        }
        return false;
    }

    // Leech Bonder: a -1/-1 counter from its side to their best creature, else a +1/+1 counter from theirs to its own
    private static boolean moveCounter(final Player ai, final SpellAbility sa) {
        Card from = null;
        Card to = null;
        for (final Card c : ai.getCreaturesInPlay()) {
            if (sa.canTarget(c) && c.getCounters(CounterEnumType.M1M1) > 0) {
                from = c;
                to = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getOpponents().getCreaturesInPlay(), sa::canTarget));
                break;
            }
        }
        if (to == null) {
            from = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getOpponents().getCreaturesInPlay(),
                    c -> sa.canTarget(c) && c.getCounters(CounterEnumType.P1P1) > 0));
            to = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCreaturesInPlay(), sa::canTarget));
        }
        if (from == null || to == null) {
            return false;
        }
        sa.getTargets().add(from);
        sa.getTargets().add(to);
        return true;
    }
}
