package forge.ai;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Hot Fix: the AI rearranges its library against the clock the way a person could in ten seconds - it pulls the
 * few cards it most wants to draw next to the top, in order, and leaves the rest as they were. It always lets go
 * in time.
 */
public final class AiTimedRearrange {
    /** Roughly how many cards a person finds and moves in ten seconds. */
    private static final int CARDS_PER_TEN_SECONDS = 5;

    private AiTimedRearrange() { }

    public static Pair<CardCollectionView, Boolean> arrange(final Player ai, final CardCollectionView cards, final int seconds) {
        final int moves = Math.max(1, CARDS_PER_TEN_SECONDS * seconds / 10);
        final List<Card> rest = Lists.newArrayList(cards);
        final CardCollection top = new CardCollection();

        int lands = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), Card::isLand)
                + CardLists.count(ai.getCardsIn(ZoneType.Hand), Card::isLand);
        int biggest = 3;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.isLand()) {
                biggest = Math.max(biggest, c.getCMC());
            }
        }

        while (top.size() < moves && !rest.isEmpty()) {
            Card pick = null;
            if (lands < Math.min(biggest, 7)) {
                pick = rest.stream().filter(Card::isLand).findFirst().orElse(null);
            }
            if (pick == null) {
                final int mana = lands + 1;
                pick = rest.stream().filter(c -> !c.isLand() && c.getCMC() <= mana)
                        .max((a, b) -> Integer.compare(ComputerUtilCard.evaluateCardImpact(a), ComputerUtilCard.evaluateCardImpact(b)))
                        .orElse(null);
            }
            if (pick == null) {
                pick = rest.stream().filter(Card::isLand).findFirst().orElse(null);
            }
            if (pick == null) {
                break;
            }
            rest.remove(pick);
            top.add(pick);
            if (pick.isLand()) {
                lands++;
            } else {
                biggest = Math.max(biggest, pick.getCMC());
            }
        }
        top.addAll(rest);
        return Pair.of(top, false);
    }
}
