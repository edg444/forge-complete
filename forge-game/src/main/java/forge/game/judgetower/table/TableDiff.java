package forge.game.judgetower.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import forge.game.judgetower.table.Observed.CardObs;
import forge.game.judgetower.table.Observed.PlayerObs;
import forge.game.judgetower.table.Observed.StackObs;
import forge.game.zone.ZoneType;

/** Where the table and the judge's game don't agree. The judge's game is what the rules say should be there. */
public final class TableDiff {
    public enum Kind { ZONE, TOWER, TAPPED, PHASED, FACE, COUNTERS, ATTACHED, CONTROL, ATTACK, BLOCK, PLAYER, STACK, EXTRA, MISSING }

    /**
     * @param id the card's id on the table, the seat for a player's difference, the item's position on the stack
     * @param seat who it would be charged to if it's missed: the affected permanent's controller, or the player it's about
     */
    public record Item(Kind kind, int id, int seat, String text) {
        public String key() {
            return kind + ":" + id;
        }
    }

    private TableDiff() { }

    public static List<Item> compare(final Observed table, final Observed judge) {
        final List<Item> items = new ArrayList<>();
        for (final Map.Entry<Integer, CardObs> e : judge.cards.entrySet()) {
            final CardObs j = e.getValue();
            final CardObs t = table.cards.get(e.getKey());
            if (t == null) {
                items.add(new Item(Kind.MISSING, j.id(), seatFor(null, j), j.name() + " should be " + where(j) + ", but isn't on the table."));
                continue;
            }
            compareCard(t, j, items);
        }
        for (final CardObs t : table.cards.values()) {
            if (!judge.cards.containsKey(t.id())) {
                items.add(new Item(Kind.EXTRA, t.id(), t.controller(), t.name() + " is " + where(t) + ", but shouldn't be there at all."));
            }
        }
        // only the order of what's in the Tower on both sides: a card that's in the wrong place has its own difference
        final Set<Integer> inBoth = new HashSet<>(table.tower);
        inBoth.retainAll(judge.tower);
        if (!table.tower.stream().filter(inBoth::contains).toList().equals(judge.tower.stream().filter(inBoth::contains).toList())) {
            items.add(new Item(Kind.TOWER, -1, -1, "The Tower isn't in the order it should be."));
        }
        for (int i = 0; i < judge.players.size(); i++) {
            final PlayerObs j = judge.players.get(i);
            final PlayerObs t = table.players.get(i);
            if (j.lost() || t.lost()) {
                continue;
            }
            if (j.life() != t.life()) {
                items.add(new Item(Kind.PLAYER, j.seat() * 10, j.seat(), j.name() + "'s life should be " + j.life() + ", not " + t.life() + "."));
            }
            if (j.poison() != t.poison()) {
                items.add(new Item(Kind.PLAYER, j.seat() * 10 + 1, j.seat(), j.name() + " should have " + j.poison() + " poison counters, not " + t.poison() + "."));
            }
            if (!j.counters().equals(t.counters())) {
                items.add(new Item(Kind.PLAYER, j.seat() * 10 + 2, j.seat(), j.name() + " should have the counters " + j.counters() + ", not " + t.counters() + "."));
            }
        }
        compareStack(table.stack, judge.stack, items);
        return items;
    }

    private static void compareCard(final CardObs t, final CardObs j, final List<Item> items) {
        final int seat = seatFor(t, j);
        if (t.zone() != j.zone() || t.zoneSeat() != j.zoneSeat()) {
            items.add(new Item(Kind.ZONE, j.id(), seat, j.name() + " should be " + where(j) + ", but is " + where(t) + "."));
            return; // what it's like there doesn't mean anything once it's in the wrong place
        }
        if (j.zone() != ZoneType.Battlefield) {
            return;
        }
        if (t.tapped() != j.tapped()) {
            items.add(new Item(Kind.TAPPED, j.id(), seat, j.name() + " should be " + (j.tapped() ? "tapped." : "untapped.")));
        }
        if (t.phasedOut() != j.phasedOut()) {
            items.add(new Item(Kind.PHASED, j.id(), seat, j.name() + " should be phased " + (j.phasedOut() ? "out." : "in.")));
        }
        if (t.faceDown() != j.faceDown() || (!j.faceDown() && !t.state().equals(j.state()))) {
            items.add(new Item(Kind.FACE, j.id(), seat, j.name() + " should be " + (j.faceDown() ? "face down." : "showing its " + j.state() + " face.")));
        }
        if (!t.counters().equals(j.counters())) {
            items.add(new Item(Kind.COUNTERS, j.id(), seat, j.name() + " should have the counters " + j.counters() + ", not " + t.counters() + "."));
        }
        if (!Objects.equals(t.attachedTo(), j.attachedTo())) {
            items.add(new Item(Kind.ATTACHED, j.id(), seat, j.name() + " is attached wrongly."));
        }
        if (t.controller() != j.controller()) {
            items.add(new Item(Kind.CONTROL, j.id(), seat, j.name() + " is under the wrong player's control."));
        }
        if (!Objects.equals(t.attacking(), j.attacking())) {
            items.add(new Item(Kind.ATTACK, j.id(), seat, j.name() + (j.attacking() != null ? " should be attacking there." : " shouldn't be attacking there.")));
        }
        if (!t.blocking().equals(j.blocking())) {
            items.add(new Item(Kind.BLOCK, j.id(), seat, j.name() + " should be blocking something else."));
        }
    }

    private static void compareStack(final List<StackObs> table, final List<StackObs> judge, final List<Item> items) {
        // trigger order within what's there is the player's to choose, and answered from the table, so position counts
        for (int i = 0; i < Math.max(table.size(), judge.size()); i++) {
            final StackObs t = i < table.size() ? table.get(i) : null;
            final StackObs j = i < judge.size() ? judge.get(i) : null;
            if (t == null) {
                items.add(new Item(Kind.STACK, i, j.controller(), j.sourceName() + "'s \"" + j.text() + "\" should be on the stack."));
            } else if (j == null) {
                items.add(new Item(Kind.STACK, i, t.controller(), t.sourceName() + "'s \"" + t.text() + "\" shouldn't be on the stack."));
            } else if (t.source() != j.source() || t.controller() != j.controller()) {
                items.add(new Item(Kind.STACK, i, j.controller(), j.sourceName() + "'s \"" + j.text() + "\" should be on the stack there, not " + t.sourceName() + "'s."));
            } else if (!t.targets().equals(j.targets())) {
                items.add(new Item(Kind.STACK, i, j.controller(), j.sourceName() + "'s \"" + j.text() + "\" has the wrong targets."));
            }
        }
    }

    /** A permanent's controller answers for it (user ruling), and whoever it should have gone to for a card it's in the wrong place. */
    private static int seatFor(final CardObs t, final CardObs j) {
        if (t != null && t.zone() == ZoneType.Battlefield) {
            return t.controller();
        }
        if (j.zone() == ZoneType.Battlefield || j.zone() == ZoneType.Hand) {
            return j.zoneSeat();
        }
        return j.controller() >= 0 ? j.controller() : j.owner();
    }

    private static String where(final CardObs c) {
        return switch (c.zone()) {
            case Battlefield -> "on the battlefield";
            case Hand -> "in a hand";
            case Library -> "in the Tower";
            case Graveyard -> "in the graveyard";
            case Exile -> "in exile";
            case Stack -> "on the stack";
            default -> "in the " + c.zone();
        };
    }
}
