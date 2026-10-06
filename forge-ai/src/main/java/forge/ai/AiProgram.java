package forge.ai;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Grand Calcutron: the AI's order for a program, where only the first card can be played. A card that can't be
 * played yet blocks everything behind it, so the order is planned turn by turn: a land (if one is left), then the
 * spells that fit the mana that turn, biggest first.
 */
public final class AiProgram {
    private AiProgram() { }

    public static CardCollection idealOrder(final Player ai, final Iterable<Card> cards) {
        final List<Card> lands = Lists.newArrayList();
        final List<Card> spells = Lists.newArrayList();
        for (final Card c : cards) {
            (c.isLand() ? lands : spells).add(c);
        }
        spells.sort(Comparator.comparingInt((Card c) -> c.getCMC()).reversed()
                .thenComparing(Comparator.comparingInt(ComputerUtilCard::evaluateCardImpact).reversed()));

        int mana = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> !c.getManaAbilities().isEmpty());
        final boolean myTurnNow = ai.getGame().getPhaseHandler().isPlayerTurn(ai);
        boolean landAllowed = !myTurnNow || ai.getMaxLandPlaysInfinite() || ai.getLandsPlayedThisTurn() < ai.getMaxLandPlays();
        int budget = myTurnNow ? ComputerUtilMana.getAvailableManaEstimate(ai, false) : mana;

        final CardCollection order = new CardCollection();
        while (!lands.isEmpty() || !spells.isEmpty()) {
            if (landAllowed && !lands.isEmpty()) {
                order.add(lands.remove(0));
                mana++;
                budget++;
            }
            boolean cast = false;
            for (final Card s : Lists.newArrayList(spells)) {
                if (s.getCMC() <= budget) {
                    order.add(s);
                    spells.remove(s);
                    budget -= s.getCMC();
                    cast = true;
                }
            }
            if (!cast && lands.isEmpty() && !spells.isEmpty()) {
                // nothing left to ramp with: queue the cheapest and hope for more mana
                final Card cheapest = spells.remove(spells.size() - 1);
                order.add(cheapest);
                mana = Math.max(mana, cheapest.getCMC());
            }
            landAllowed = true;
            budget = mana;
        }
        return order;
    }

    /** Before the first card the ideal order puts after it, so the program drifts toward that order. */
    public static int insertPosition(final Player ai, final Card card, final CardCollectionView program) {
        final List<Card> all = Lists.newArrayList(program);
        all.add(card);
        final CardCollection ideal = idealOrder(ai, all);
        final int rank = ideal.indexOf(card);
        for (int i = 0; i < program.size(); i++) {
            if (ideal.indexOf(program.get(i)) > rank) {
                return i;
            }
        }
        return program.size();
    }
}
