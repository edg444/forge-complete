package forge.ai.ability;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;

/**
 * Hidden-zone picks a logic settled when it decided: the card it meant (Flash's affordable creature), or the worst
 * cards when it's choosing for an opponent (Jester's Mask).
 */
public final class ZonePickAi {

    private static final Map<Integer, List<Card>> CARDS = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> WORST = new ConcurrentHashMap<>();

    private ZonePickAi() { }

    public static void plan(final Card host, final List<Card> cards) {
        CARDS.put(host.getId(), cards);
        WORST.remove(host.getId());
    }

    public static void planWorst(final Card host) {
        WORST.put(host.getId(), true);
        CARDS.remove(host.getId());
    }

    public static boolean planned(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        return host != null && (CARDS.containsKey(host.getId()) || WORST.containsKey(host.getId()))
                && (MiscSpellAi.handles(sa.getRootAbility().getParam("AILogic")) || ExtraCardAi.handles(sa.getRootAbility().getParam("AILogic"))
                || EndgameAi.handles(sa.getRootAbility().getParam("AILogic")));
    }

    private static int worth(final Card c) {
        return c.isLand() ? 0 : c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 50 + c.getCMC() * 20;
    }

    /** The next card to pick from the options. */
    public static Card pick(final SpellAbility sa, final CardCollectionView options) {
        final int id = sa.getHostCard().getId();
        if (WORST.containsKey(id)) {
            return options.stream().min(Comparator.comparingInt(ZonePickAi::worth)).orElse(null);
        }
        for (final Card c : CARDS.getOrDefault(id, List.of())) {
            if (options.contains(c)) {
                return c;
            }
        }
        return null;
    }
}
