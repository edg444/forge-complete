package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ DiscardForX (Nostalgic Dreams, Restless Dreams, Sickening Dreams, Turbulent Dreams, Vengeful Dreams): "As an
 * additional cost, discard X cards". X never goes past the cards it would discard anyway, so each one is traded for a
 * target worth having - their attackers exiled, their best permanents bounced, its best cards back from the graveyard,
 * or the X that does the most as a sweep.
 */
public final class DiscardForXAi {

    private DiscardForXAi() { }

    private static int spareCards(final Player ai, final Card host) {
        final CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        hand.remove(host);
        int n = 0;
        for (Card c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand); c != null;
                c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand)) {
            hand.remove(c);
            n++;
        }
        return n;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int spare = spareCards(ai, host);
        if (spare == 0) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (sa.getApi() == ApiType.DamageAll) {
            // Sickening Dreams: X to each creature and each player
            int best = Integer.MIN_VALUE;
            int bestX = 0;
            for (int x = 1; x <= spare; x++) {
                int v = 0;
                for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                    if (c.isCreature() && x >= c.getLethalDamage() && c.canBeDestroyed()) {
                        v += (c.getController().isOpponentOf(ai) ? 1 : -1) * ComputerUtilCard.evaluateCreature(c);
                    }
                }
                if (x >= ai.getLife()) {
                    v -= 10000;
                }
                for (final Player p : ai.getOpponents()) {
                    v += x >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife() ? 1000 : x * 6;
                }
                v -= x * 6 + x * 40;
                if (v > best) {
                    best = v;
                    bestX = x;
                }
            }
            if (best < 60) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.setXManaCostPaid(bestX);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (sa.getApi() != ApiType.ChangeZone || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final String origin = sa.getParamOrDefault("Origin", "");
        sa.setXManaCostPaid(spare);
        sa.resetTargets();
        final List<Card> options = new ArrayList<>(CardUtil.getValidCardsToTarget(sa));
        final List<Card> picks = new ArrayList<>();
        if (origin.contains("Graveyard")) {
            // Restless Dreams, Nostalgic Dreams: its best cards back
            options.removeIf(c -> !c.getOwner().equals(ai));
            options.sort(Comparator.comparingInt(DiscardForXAi::cardValue).reversed());
            options.stream().filter(c -> cardValue(c) >= 60).limit(spare).forEach(picks::add);
        } else {
            final Combat combat = ai.getGame().getCombat();
            final boolean exile = sa.getParamOrDefault("Destination", "").contains("Exile");
            if (exile && (combat == null || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) || ph.isPlayerTurn(ai))) {
                // Vengeful Dreams: their attackers, once blocks are known
                return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
            }
            if (!exile && !(ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1)) && !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai))) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
            options.removeIf(c -> !c.getController().isOpponentOf(ai));
            options.sort(Comparator.comparingInt((Card c) -> bounceValue(c, exile)).reversed());
            options.stream().filter(c -> bounceValue(c, exile) >= 80).limit(spare).forEach(picks::add);
        }
        if (picks.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(picks.size());
        sa.resetTargets();
        picks.forEach(c -> sa.getTargets().add(c));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static int cardValue(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 30 + c.getCMC() * 25;
    }

    private static int bounceValue(final Card c, final boolean exile) {
        final int v = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
        return exile || c.isToken() ? v : v / 3 + c.getCMC() * 15 + c.getAttachedCards().size() * 40;
    }
}
