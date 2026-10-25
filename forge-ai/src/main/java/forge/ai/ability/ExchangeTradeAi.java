package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.simulation.GameStateEvaluator;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Exchanges of control the AI weighs as a trade.
 * <ul>
 * <li>AILogic$ ExchangePair (Legerdemain, Political Trickery, Shrewd Negotiation, Spawnbroker, Puca's Mischief,
 * Vedalken Plotter, Shifting Borders): the best pair of one of its permanents for one of theirs that the targeting
 * allows, worth what theirs is worth over what it gives.</li>
 * <li>AILogic$ SwapArmies (Reins of Power, Twist Allegiance): both sides' creatures change hands until end of turn - cast
 * in its own first main phase when their creatures, attacking into its own, are lethal.</li>
 * </ul>
 */
public final class ExchangeTradeAi {

    private ExchangeTradeAi() { }

    public static boolean handles(final String logic) {
        return "ExchangePair".equals(logic) || "SwapArmies".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return "SwapArmies".equals(sa.getParam("AILogic")) ? swapArmies(ai, sa) : pair(ai, sa, mandatory);
    }

    static int value(final Card c) {
        if (c.isCreature()) {
            return ComputerUtilCard.evaluateCreature(c);
        }
        if (c.isLand()) {
            return GameStateEvaluator.evaluateLand(c);
        }
        return (c.getCMC() + 1) * 25;
    }

    private static int trade(final Player ai, final Card a, final Card b) {
        if (a.getController().equals(ai) && b.getController().isOpponentOf(ai)) {
            return value(b) - value(a);
        }
        if (b.getController().equals(ai) && a.getController().isOpponentOf(ai)) {
            return value(a) - value(b);
        }
        return Integer.MIN_VALUE;
    }

    private static AiAbilityDecision pair(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final List<SpellAbility> tgts = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                tgts.add(sub);
            }
        }
        if (tgts.isEmpty() || tgts.size() > 2) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final SpellAbility first = tgts.get(0);
        // Shifting Borders takes both lands with one "target"
        final SpellAbility second = tgts.size() == 2 ? tgts.get(1) : first;
        int best = Integer.MIN_VALUE;
        Card bestA = null;
        Card bestB = null;
        for (final SpellAbility t : tgts) {
            t.resetTargets();
        }
        for (final Card a : CardUtil.getValidCardsToTarget(first)) {
            first.getTargets().add(a);
            if (second != first) {
                second.resetTargets();
            }
            // the second target is checked against the first (shared type, lesser power or mana value)
            for (final Card b : CardUtil.getValidCardsToTarget(second)) {
                if (b.equals(a)) {
                    continue;
                }
                final int v = trade(ai, a, b);
                if (v > best) {
                    best = v;
                    bestA = a;
                    bestB = b;
                }
            }
            first.resetTargets();
        }
        final int threshold = mandatory ? Integer.MIN_VALUE + 1 : sa.isSpell() ? 60 : sa.isTrigger() ? 20 : 40;
        if (bestA == null || best < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        first.getTargets().add(bestA);
        second.getTargets().add(bestB);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision swapArmies(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        for (final Player opp : ai.getOpponents()) {
            if (!sa.canTarget(opp) || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
                continue;
            }
            // what it can do now, against what their creatures do (untapped, hasted) into its own
            final List<Card> mine = CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
            final List<Card> theirBlockers = CardLists.filter(opp.getCreaturesInPlay(), CombatUtil::canBlock);
            if (ForcedCombatAi.assault(mine, theirBlockers, opp) >= opp.getLife()) {
                continue;
            }
            final List<Card> borrowed = new ArrayList<>(opp.getCreaturesInPlay());
            final List<Card> lent = new ArrayList<>(ai.getCreaturesInPlay());
            if (ForcedCombatAi.assault(borrowed, lent, opp) >= opp.getLife()) {
                sa.getTargets().add(opp);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
