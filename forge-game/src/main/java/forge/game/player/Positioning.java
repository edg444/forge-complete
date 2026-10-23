package forge.game.player;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Defender of the Queue's positioning: while a player controls a creature with positioning, their creatures are locked
 * in a left-to-right order. Locking orders the creatures already there; each creature that enters or comes under
 * their control afterwards is put to the left or right of one already in line, one at a time (rulings). A person picks
 * the neighbor and side; the AI puts newcomers beside its positioning creature (user ruling). Kept in step whenever
 * state-based actions are checked; each creature shows its place.
 */
public final class Positioning {

    private Positioning() { }

    public static void sync(final Game game) {
        for (final Player p : game.getPlayers()) {
            sync(p);
        }
    }

    private static void sync(final Player p) {
        final List<Card> order = p.getCreatureOrder();
        final CardCollection creatures = p.getCreaturesInPlay();
        final boolean active = creatures.anyMatch(c -> c.hasKeyword(Keyword.POSITIONING));
        if (!active) {
            if (!order.isEmpty()) {
                for (final Card c : order) {
                    c.setWrittenDown(null);
                }
                order.clear();
            }
            return;
        }
        boolean changed = order.removeIf(c -> !creatures.contains(c) || !c.equalsWithGameTimestamp(p.getGame().getCardState(c)));
        final CardCollection newcomers = CardLists.filter(creatures, c -> !order.contains(c));
        if (order.isEmpty() && !newcomers.isEmpty()) {
            // locking: the creatures already there, in an order of their controller's choosing
            final CardCollection already = CardLists.filter(newcomers, c -> !c.hasKeyword(Keyword.POSITIONING) || newcomers.size() == 1);
            if (!already.isEmpty()) {
                final List<Card> locked = p.getController().isAI() ? Lists.newArrayList(already)
                        : Lists.newArrayList(p.getController().orderMoveToZoneList(already, ZoneType.Battlefield, null));
                order.addAll(locked);
                newcomers.removeAll(locked);
                changed = true;
            }
        }
        for (final Card c : newcomers) {
            place(p, order, c);
            changed = true;
        }
        if (changed) {
            for (int i = 0; i < order.size(); i++) {
                order.get(i).setWrittenDown("position " + (i + 1) + " of " + order.size() + " from the left");
            }
        }
    }

    private static void place(final Player p, final List<Card> order, final Card c) {
        if (order.isEmpty()) {
            order.add(c);
            return;
        }
        Card neighbor;
        boolean left;
        if (p.getController().isAI()) {
            neighbor = order.stream().filter(x -> x.hasKeyword(Keyword.POSITIONING)).findFirst().orElse(order.get(order.size() - 1));
            left = false;
        } else {
            final SpellAbility sa = new SpellAbility.EmptySa(c, p);
            neighbor = p.getController().chooseSingleEntityForEffect(new CardCollection(order), sa,
                    "Position " + c.getTranslatedName() + " next to which creature?", null);
            if (neighbor == null) {
                neighbor = order.get(order.size() - 1);
            }
            left = p.getController().chooseBinary(sa, "Put " + c.getTranslatedName() + " to the left or the right of "
                    + neighbor.getTranslatedName() + "?", PlayerController.BinaryChoiceType.LeftOrRight, (Boolean) null);
        }
        final int at = order.indexOf(neighbor);
        order.add(left ? at : at + 1, c);
    }

    /** True if a and b are next to each other in their controller's line. */
    public static boolean adjacent(final Card a, final Card b) {
        if (a.getController() == null || !a.getController().equals(b.getController())) {
            return false;
        }
        final List<Card> order = a.getController().getCreatureOrder();
        final int i = order.indexOf(a);
        final int j = order.indexOf(b);
        return i >= 0 && j >= 0 && Math.abs(i - j) == 1;
    }
}
