package forge.game;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import forge.card.CardStateName;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageHistory;
import forge.game.card.CardState;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.event.GameEventZone;
import forge.game.keyword.KeywordCollection;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

/**
 * Do-Over: "Restart the turn." The game as it stood between the last turn and this one is kept - only in a game with a
 * card that can restart a turn - and restarting puts it all back: every card where it was and as it was (counters,
 * damage, tapped, effects on it), every player's life, counters and turn tallies, the turn's pending triggers and
 * effect timers, and nothing made since (tokens, copies, effect objects, delayed triggers, "until end of turn"s). The
 * turn then begins again with its untap step. The card's "reconstruct as best you can" is exact here: the engine knows
 * every hidden card. Rulings: undoing isn't happening - no abilities trigger and undone damage or life gain counts as
 * nothing.
 *
 * <p>The objects are restored in place - the same card objects the turn began with, field by field - so anything from
 * before the turn that refers to them (an opponent's "until your next turn", a delayed trigger, a leaves-play timer)
 * still finds them. Fields are copied one level deep, with collections and maps copied by contents, and a few kinds of
 * objects a card or player owns (its states, damage history, keyword lists, mana pool, life total) copied as well.
 */
public final class TurnRestart {

    /** Whether anything in the game can restart the turn, so the turn's start is worth keeping. */
    public static boolean anyCanRestart(final Game game) {
        for (final Player p : game.getPlayers()) {
            for (final Card c : p.getAllCards()) {
                for (final CardStateName name : c.getStates()) {
                    for (final SpellAbility sa : c.getState(name).getSpellAbilities()) {
                        if (sa.getApi() == ApiType.RestartTurn) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private final List<Fields> saved = new ArrayList<>();
    private final Set<Player> inGame;

    private TurnRestart(final Game game) {
        inGame = ImmutableSet.copyOf(game.getPlayers());
        saved.add(new Fields(game, f -> GAME_FIELDS.contains(f.getName())));
        saved.add(new Fields(game.getPhaseHandler(), f -> !PHASE_HANDLER_SKIPPED.contains(f.getName())));
        for (final Object phase : new Object[] {game.untap, game.upkeep, game.beginOfCombat, game.endOfCombat, game.endOfTurn, game.cleanup}) {
            saved.add(new Fields(phase, f -> !f.getName().equals("type")));
        }
        saved.add(new Fields(game.getTriggerHandler(), f -> !TRIGGER_HANDLER_SKIPPED.contains(f.getName())));
        saved.add(new Fields(game.getStack(), f -> STACK_FIELDS.contains(f.getName())));
        saved.add(new Fields(game.getStaticEffects(), f -> true));

        final List<Zone> zones = new ArrayList<>();
        zones.add(game.getStackZone());
        for (final Player p : game.getPlayers()) {
            saved.add(new Fields(p, f -> !PLAYER_SKIPPED.contains(f.getName())));
            zones.addAll(p.getAllZones());
        }
        final Map<Card, Boolean> cards = new IdentityHashMap<>();
        for (final Zone z : zones) {
            saved.add(new Fields(z, f -> !ZONE_SKIPPED.contains(f.getName())));
            for (final Card c : z.getCards(false)) {
                cards.put(c, true);
            }
            if (z instanceof PlayerZoneBattlefield bf) {
                for (final Card c : bf.getMeldedCards()) {
                    cards.put(c, true);
                }
            }
        }
        for (final Card c : cards.keySet()) {
            saved.add(new Fields(c, f -> !CARD_SKIPPED.contains(f.getName())));
        }
    }

    /** The game as it stands now, between turns. */
    public static TurnRestart capture(final Game game) {
        return new TurnRestart(game);
    }

    /**
     * Puts the game back as it was between turns, except that {@code exiled} (Do-Over) goes to exile. The caller
     * then begins the turn.
     */
    public void restore(final Game game, final Card exiled) {
        game.fireEvent(new GameEventSnapshotRestored(true));
        game.getTriggerHandler().clearWaitingTriggers();
        game.getStack().clear();
        game.getStack().clearSimultaneousStack();

        for (final Fields f : saved) {
            f.restore();
        }

        game.getTriggerHandler().setSuppressAllTriggers(true);
        // a player who has left the game since stays out of it, and so do the cards they own (CR 800.4a)
        for (final Player gone : inGame) {
            if (!game.getPlayers().contains(gone)) {
                for (final Card c : game.getCardsInGame()) {
                    if (c.getOwner() == gone && c.getZone() != null) {
                        c.getZone().remove(c);
                    }
                }
            }
        }

        if (exiled != null) {
            final Card then = game.findById(exiled.getId());
            if (then != null) {
                game.getAction().exile(then, null, null);
            } else if (!exiled.isCopiedSpell() && !exiled.isToken()) {
                // it wasn't in the game as the turn began (conjured, say), but it's a card all the same
                game.getAction().exile(exiled, null, null);
            }
        }

        // passing priority until the turn ended meant the turn now undone
        for (final Player p : game.getPlayers()) {
            p.getController().autoPassCancel();
        }

        game.getTriggerHandler().resetActiveTriggers();
        game.getAction().checkStaticAbilities();
        game.getTriggerHandler().setSuppressAllTriggers(false);
        game.copyLastState();

        refreshViews(game);
        game.fireEvent(new GameEventSnapshotRestored(false));
    }

    private static void refreshViews(final Game game) {
        final CardCollection all = new CardCollection(game.getCardsInGame());
        all.addAll(game.getCardsIn(ZoneType.Stack));
        for (final Card c : all) {
            c.updateAllForView();
        }
        for (final Player p : game.getPlayers()) {
            p.updateAllForView();
        }
        game.updatePhaseForView();
        game.updateTurnForView();
        game.updatePlayerTurnForView();
        game.updateCombatForView();
        game.updateStackForView();

        for (final Player p : game.getPlayers()) {
            for (final PlayerZone z : p.getAllZones()) {
                game.fireEvent(new GameEventZone(z.getZoneType(), p, EventValueChangeType.ComplexUpdate, null));
            }
        }
        game.fireEvent(new GameEventCardStatsChanged(forge.game.card.CardView.getCollection(all), false));
        game.fireEvent(new GameEventPlayerStatsChanged(game.getPlayers()));
    }

    // Game: only its state of play - not its players, handlers, views, ids or clock
    private static final Set<String> GAME_FIELDS = ImmutableSet.of("activePlanes", "sbaCheckedCommandList",
            "untilHostLeavesPlayTriggerList", "countersAddedThisTurn", "countersRemovedThisTurn",
            "leftBattlefieldThisTurn", "leftGraveyardThisTurn", "globalDamageHistory", "damageThisTurnLKI", "topLibsCast",
            "facedownWhileCasting", "initiative", "monarch", "monarchBeginTurn", "turnOrder", "daytime",
            "numPiledGuessedSA", "numberChangeFrom", "numberChangeTo", "changeZoneLKIInfo");
    private static final Set<String> PHASE_HANDLER_SKIPPED = ImmutableSet.of("sw", "game", "turnStart", "pendingRestart");
    private static final Set<String> TRIGGER_HANDLER_SKIPPED = ImmutableSet.of("game", "suppressedModes", "allSuppressed");
    private static final Set<String> STACK_FIELDS = ImmutableSet.of("simultaneousStackEntryList", "activePlayerSAs",
            "thisTurnCast", "lastTurnCast", "thisTurnActivated", "triggerRepeats", "maxDistinctSources");
    private static final Set<String> PLAYER_SKIPPED = ImmutableSet.of("game", "view", "controller", "stats",
            "achievementTracker", "outsideRegistration");
    private static final Set<String> ZONE_SKIPPED = ImmutableSet.of("zoneType", "game", "player");
    private static final Set<String> CARD_SKIPPED = ImmutableSet.of("game", "view", "id");
    private static final Set<String> NESTED_SKIPPED = ImmutableSet.of("view", "card", "owner");

    /** Objects a card or player owns outright, copied along with it. */
    private static boolean ownedOutright(final Object o) {
        return o instanceof CardState || o instanceof CardDamageHistory || o instanceof KeywordCollection
                || o instanceof ManaPool || o.getClass().getName().equals("forge.game.player.Player$LifeTotal");
    }

    /** One object's fields, one level deep. */
    private static final class Fields {
        private static final Map<Class<?>, Field[]> FIELDS = new ConcurrentHashMap<>();

        private final Object target;
        private final Field[] fields;
        private final Object[] values;
        private final Runnable[] contents;
        private final List<Fields> owned = new ArrayList<>();

        Fields(final Object target, final Predicate<Field> include) {
            this.target = target;
            final List<Field> chosen = new ArrayList<>();
            for (final Field f : allFields(target.getClass())) {
                if (include.test(f)) {
                    chosen.add(f);
                }
            }
            fields = chosen.toArray(new Field[0]);
            values = new Object[fields.length];
            contents = new Runnable[fields.length];
            for (int i = 0; i < fields.length; i++) {
                final Object v = read(fields[i], target);
                values[i] = v;
                contents[i] = contents(v);
                own(v);
            }
        }

        private void own(final Object v) {
            if (v == null) {
                return;
            }
            if (ownedOutright(v)) {
                owned.add(new Fields(v, f -> !NESTED_SKIPPED.contains(f.getName())));
            } else if (v instanceof Map<?, ?> m) {
                for (final Object value : m.values()) {
                    if (value != null && ownedOutright(value)) {
                        owned.add(new Fields(value, f -> !NESTED_SKIPPED.contains(f.getName())));
                    }
                }
            }
        }

        void restore() {
            for (int i = 0; i < fields.length; i++) {
                if (!Modifier.isFinal(fields[i].getModifiers())) {
                    try {
                        fields[i].set(target, values[i]);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
                if (contents[i] != null) {
                    contents[i].run();
                }
            }
            for (final Fields f : owned) {
                f.restore();
            }
        }

        private static Field[] allFields(final Class<?> cls) {
            return FIELDS.computeIfAbsent(cls, c -> {
                final List<Field> list = new ArrayList<>();
                for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                    for (final Field f : k.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
                            continue;
                        }
                        f.setAccessible(true);
                        list.add(f);
                    }
                }
                return list.toArray(new Field[0]);
            });
        }

        private static Object read(final Field f, final Object target) {
            try {
                return f.get(target);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        /** Puts a collection's, map's or array's contents back as they are now; null for anything else. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Runnable contents(final Object v) {
            if (v instanceof Collection c) {
                final Object[] elems = c.toArray();
                return () -> refill(c, elems);
            }
            if (v instanceof Map m) {
                final Object[] keys = m.keySet().toArray();
                final Object[] vals = new Object[keys.length];
                final List<Runnable> inner = new ArrayList<>();
                for (int i = 0; i < keys.length; i++) {
                    vals[i] = m.get(keys[i]);
                    if (vals[i] instanceof Collection || vals[i] instanceof Map) {
                        inner.add(contents(vals[i]));
                    }
                }
                return () -> {
                    try {
                        m.clear();
                        for (int i = 0; i < keys.length; i++) {
                            m.put(keys[i], vals[i]);
                        }
                    } catch (final UnsupportedOperationException e) {
                        return; // unmodifiable, so unchanged
                    }
                    inner.forEach(Runnable::run);
                };
            }
            if (v instanceof Multimap mm) {
                final Object[] entries = mm.entries().stream().map(e -> {
                    final Map.Entry en = (Map.Entry) e;
                    return new Object[] {en.getKey(), en.getValue()};
                }).toArray();
                return () -> {
                    try {
                        mm.clear();
                        for (final Object e : entries) {
                            mm.put(((Object[]) e)[0], ((Object[]) e)[1]);
                        }
                    } catch (final UnsupportedOperationException e) {
                        // unmodifiable, so unchanged
                    }
                };
            }
            if (v instanceof Table t) {
                final List<Object[]> cells = new ArrayList<>();
                final List<Runnable> inner = new ArrayList<>();
                for (final Object o : t.cellSet()) {
                    final Table.Cell cell = (Table.Cell) o;
                    cells.add(new Object[] {cell.getRowKey(), cell.getColumnKey(), cell.getValue()});
                    if (cell.getValue() instanceof Collection || cell.getValue() instanceof Map) {
                        inner.add(contents(cell.getValue()));
                    }
                }
                return () -> {
                    try {
                        t.clear();
                        for (final Object[] cell : cells) {
                            t.put(cell[0], cell[1], cell[2]);
                        }
                    } catch (final UnsupportedOperationException e) {
                        return; // unmodifiable, so unchanged
                    }
                    inner.forEach(Runnable::run);
                };
            }
            if (v != null && v.getClass().isArray()) {
                final int length = Array.getLength(v);
                final Object copy = Array.newInstance(v.getClass().getComponentType(), length);
                System.arraycopy(v, 0, copy, 0, length);
                return () -> System.arraycopy(copy, 0, v, 0, length);
            }
            return null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void refill(final Collection c, final Object[] elems) {
            if (c.isEmpty() && elems.length == 0) {
                return;
            }
            try {
                c.clear();
                Collections.addAll(c, elems);
            } catch (final UnsupportedOperationException e) {
                // unmodifiable, so unchanged
            }
        }
    }
}
