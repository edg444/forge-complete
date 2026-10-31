package forge.game.judgetower.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.ToIntFunction;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

/**
 * What can be seen on a table, read the same way from the table and from the judge's game, so the two can be compared.
 * Damage marks and summoning sickness are left out on purpose: nobody keeps those physically in paper, so they're
 * nothing a player can be caught out on. The order of hands and graveyards doesn't count either, but the Tower's does.
 */
public final class Observed {
    public record CardObs(int id, String name, ZoneType zone, int zoneSeat, boolean tapped, boolean phasedOut,
            boolean faceDown, String state, Map<String, Integer> counters, String attachedTo, int controller, int owner,
            String attacking, Set<Integer> blocking, boolean token) { }

    public record PlayerObs(int seat, String name, int life, int poison, Map<String, Integer> counters, boolean lost) { }

    public record StackObs(int source, String sourceName, int controller, boolean spell, Set<String> targets, String text) { }

    private static final ZoneType[] ZONES = { ZoneType.Hand, ZoneType.Battlefield, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile };

    public final Map<Integer, CardObs> cards = new LinkedHashMap<>();
    /** The Tower, top first. With separate libraries (which the format doesn't have) they'd simply run on. */
    public final List<Integer> tower = new ArrayList<>();
    public final List<PlayerObs> players = new ArrayList<>();
    public final List<StackObs> stack = new ArrayList<>();

    /** @param idOf the card's id as the table knows it; negative for one the table has no counterpart of yet */
    public static Observed read(final Game game, final ToIntFunction<Card> idOf, final boolean compareLife) {
        final Observed o = new Observed();
        final Map<Player, Integer> seats = new HashMap<>();
        int seat = 0;
        for (final Player p : game.getRegisteredPlayers()) {
            seats.put(p, seat++);
        }
        final Combat combat = game.getPhaseHandler().getCombat();
        final Set<Zone> done = new HashSet<>();
        for (final Player p : game.getRegisteredPlayers()) {
            for (final ZoneType type : ZONES) {
                final Zone zone = p.getZone(type);
                if (zone == null || !done.add(zone)) {
                    continue;
                }
                // a player's own zone only where that's what it's about: exile and the shared zones belong to no one here
                final int zoneSeat = type == ZoneType.Hand || type == ZoneType.Battlefield
                        || (type == ZoneType.Graveyard && !game.isSharedZone(type)) ? seats.get(p) : -1;
                for (final Card c : zone.getCards()) {
                    o.add(c, type, zoneSeat, seats, combat, idOf);
                    if (type == ZoneType.Library) {
                        o.tower.add(idOf.applyAsInt(c));
                    }
                }
            }
        }
        for (final Card c : game.getStackZone().getCards()) {
            o.add(c, ZoneType.Stack, -1, seats, combat, idOf);
        }
        for (final SpellAbilityStackInstance si : game.getStack()) {
            final Set<String> targets = new TreeSet<>();
            for (SpellAbility sa = si.getSpellAbility(); sa != null; sa = sa.getSubAbility()) {
                addTargets(sa, targets, seats, idOf);
            }
            final SpellAbility sa = si.getSpellAbility();
            o.stack.add(new StackObs(idOf.applyAsInt(si.getSourceCard()), si.getSourceCard().getName(),
                    seats.getOrDefault(sa.getActivatingPlayer(), -1), sa.isSpell(), targets, si.getStackDescription()));
        }
        for (final Player p : game.getRegisteredPlayers()) {
            final Map<String, Integer> counters = new TreeMap<>();
            for (final CounterType t : p.getCounters().elementSet()) {
                if (!t.is(CounterEnumType.POISON)) {
                    counters.put(t.toString(), p.getCounters().count(t));
                }
            }
            o.players.add(new PlayerObs(seats.get(p), p.getName(), compareLife ? p.getLife() : 0, p.getPoisonCounters(), counters, p.hasLost()));
        }
        return o;
    }

    private void add(final Card c, final ZoneType zone, final int zoneSeat, final Map<Player, Integer> seats, final Combat combat,
            final ToIntFunction<Card> idOf) {
        final Map<String, Integer> counters = new TreeMap<>();
        for (final CounterType t : c.getCounters().elementSet()) {
            counters.put(t.toString(), c.getCounters().count(t));
        }
        String attacking = null;
        final Set<Integer> blocking = new TreeSet<>();
        if (combat != null && zone == ZoneType.Battlefield) {
            if (combat.isAttacking(c)) {
                attacking = key(combat.getDefenderByAttacker(c), seats, idOf);
            }
            if (combat.isBlocking(c)) {
                for (final Card a : combat.getAttackersBlockedBy(c)) {
                    blocking.add(idOf.applyAsInt(a));
                }
            }
        }
        final int id = idOf.applyAsInt(c);
        cards.put(id, new CardObs(id, c.getName(), zone, zoneSeat, zone == ZoneType.Battlefield && c.isTapped(), c.isPhasedOut(),
                c.isFaceDown(), zone == ZoneType.Battlefield ? c.getCurrentStateName().name() : "", counters,
                key(c.getEntityAttachedTo(), seats, idOf), seats.getOrDefault(c.getController(), -1),
                seats.getOrDefault(c.getOwner(), -1), attacking, blocking, c.isToken()));
    }

    private static void addTargets(final SpellAbility sa, final Set<String> targets, final Map<Player, Integer> seats, final ToIntFunction<Card> idOf) {
        for (final Object t : sa.getTargets()) {
            if (t instanceof SpellAbility on) {
                targets.add("S" + idOf.applyAsInt(on.getHostCard()));
            } else if (t instanceof GameEntity e) {
                targets.add(key(e, seats, idOf));
            }
        }
    }

    /** A target, a defender or what's attached to, as text that means the same in either game. */
    private static String key(final GameEntity e, final Map<Player, Integer> seats, final ToIntFunction<Card> idOf) {
        if (e == null) {
            return null;
        }
        return e instanceof Card c ? "C" + idOf.applyAsInt(c) : "P" + seats.getOrDefault(e, -1);
    }
}
