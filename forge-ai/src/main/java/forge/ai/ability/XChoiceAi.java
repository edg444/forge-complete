package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spells whose X is a mana value limit, so X is picked for the payoff rather than as high as it goes.
 * <ul>
 * <li>AILogic$ StealX (Dominate, Entrancing Melody): X is the mana value of the best creature of theirs it can afford.</li>
 * <li>AILogic$ WipeX (Forced March, Gaze of Granite): every affordable X is tried for what the sweep destroys on each
 * side, the lowest X giving the best result.</li>
 * </ul>
 */
public final class XChoiceAi {

    private XChoiceAi() { }

    public static boolean handles(final String logic) {
        return "StealX".equals(logic) || "WipeX".equals(logic) || "DestroyX".equals(logic);
    }

    private static int maxX(final Player ai, final SpellAbility sa) {
        final int base = sa.getPayCosts() == null || sa.getPayCosts().getTotalMana() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
        return ComputerUtilMana.getAvailableManaEstimate(ai, false) - base;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final int max = maxX(ai, sa);
        if (max < 0) {
            return new AiAbilityDecision(0, AiPlayDecision.CantAfford);
        }
        int best = Integer.MIN_VALUE;
        int bestX = -1;
        Card bestTarget = null;
        if ("DestroyX".equals(sa.getParam("AILogic"))) {
            // Dregs of Sorrow: as many of their worthwhile creatures as it can pay for
            sa.setXManaCostPaid(Math.max(1, max));
            sa.resetTargets();
            final java.util.List<Card> picks = CardLists.filter(forge.game.card.CardUtil.getValidCardsToTarget(sa),
                    c -> c.getController().isOpponentOf(ai) && c.canBeDestroyed() && ComputerUtilCard.evaluateCreature(c) >= 100);
            picks.sort(java.util.Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
            final int x = Math.min(max, picks.size());
            if (x < 1) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.setXManaCostPaid(x);
            sa.resetTargets();
            picks.stream().limit(x).forEach(c -> sa.getTargets().add(c));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if ("StealX".equals(sa.getParam("AILogic"))) {
            for (final Card c : CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), c -> c.isCreature() && c.getController().isOpponentOf(ai))) {
                if (c.getCMC() > max) {
                    continue;
                }
                sa.setXManaCostPaid(c.getCMC());
                sa.resetTargets();
                if (!sa.canTarget(c)) {
                    continue;
                }
                final int v = ComputerUtilCard.evaluateCreature(c);
                if (v > best) {
                    best = v;
                    bestX = c.getCMC();
                    bestTarget = c;
                }
            }
            if (bestTarget == null || best < 100) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.setXManaCostPaid(bestX);
            sa.resetTargets();
            sa.getTargets().add(bestTarget);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // Black Sun's Zenith: -1/-1 counters (or -X/-X) kill what has toughness X or less, indestructible or not
        final boolean shrinks = sa.getApi() == forge.game.ability.ApiType.PutCounterAll || sa.getApi() == forge.game.ability.ApiType.PumpAll;
        for (int x = shrinks ? 1 : 0; x <= max; x++) {
            sa.setXManaCostPaid(x);
            int v = 0;
            for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("ValidCards"), ai, sa.getHostCard(), sa)) {
                if (shrinks ? c.getNetToughness() > x : !c.canBeDestroyed()) {
                    continue;
                }
                final int w = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                v += c.getController().isOpponentOf(ai) ? w : c.getController().equals(ai) ? -w : 0;
            }
            if (v > best) {
                best = v;
                bestX = x;
            }
        }
        if (bestX < 0 || best < 150) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestX);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
