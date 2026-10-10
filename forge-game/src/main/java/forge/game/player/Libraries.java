package forge.game.player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Split Screen: players with more than one library. "If anything refers to your library, choose one of your libraries
 * for it." Rulings: a draw can come from any of them (each card drawn its own choice), an empty one only loses you the
 * game if you draw from it, a second Split Screen splits one of the four again (seven), and leaving shuffles them all
 * back together.
 * <p>
 * Each library is a {@link PlayerZone} of type Library; {@code Player.getZone(Library)} is the one "your library" means
 * right now. While an ability that mentions a library resolves, the first time it touches a player's library that
 * player chooses one, and the whole ability keeps using it (search, then shuffle the same library). Continuous effects
 * get a library per permanent, chosen at the next state-based check after they start applying or the library splits
 * (user, 2026-10-09). Anything else - a cost, say - uses the library chosen last.
 */
public final class Libraries {
    private Libraries() { }

    private static final Set<ApiType> LIBRARY_APIS = EnumSet.of(ApiType.Mill, ApiType.Scry, ApiType.Surveil, ApiType.Dig,
            ApiType.DigMultiple, ApiType.DigUntil, ApiType.RearrangeTopOfLibrary, ApiType.PeekAndReveal, ApiType.Shuffle,
            ApiType.Explore, ApiType.Manifest, ApiType.Cloak, ApiType.Discover, ApiType.Seek, ApiType.SplitLibrary);

    /** One ability resolving that refers to libraries, and the library each player chose for it. */
    public static final class Reference {
        private final SpellAbility sa;
        private final Map<Player, PlayerZone> chosen = Maps.newHashMap();
        private Reference(final SpellAbility sa) {
            this.sa = sa;
        }
    }

    /** What the game keeps: the abilities resolving right now (innermost first). */
    public static final class State {
        private final Deque<Reference> references = new ArrayDeque<>();
    }

    public static boolean anyoneHasSeveral(final Game game) {
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.hasSeveralLibraries()) {
                return true;
            }
        }
        return false;
    }

    /** Starts an ability's resolution; returns what to hand back to {@link #end}, or null when it needs nothing. */
    public static Reference begin(final SpellAbility sa) {
        if (sa == null || sa.getHostCard() == null) {
            return null;
        }
        final Game game = sa.getHostCard().getGame();
        if (!anyoneHasSeveral(game) || !refersToLibrary(sa)) {
            return null;
        }
        final Reference r = new Reference(sa);
        game.getLibraryState().references.push(r);
        return r;
    }

    public static void end(final Reference r) {
        if (r == null) {
            return;
        }
        final Game game = r.sa.getHostCard().getGame();
        game.getLibraryState().references.remove(r);
        // an ability that was resolving around this one goes back to the libraries it chose
        final Reference outer = game.getLibraryState().references.peek();
        if (outer != null) {
            for (final Map.Entry<Player, PlayerZone> e : outer.chosen.entrySet()) {
                if (e.getKey().getLibraryZones().contains(e.getValue())) {
                    e.getKey().setCurrentLibrary(e.getValue());
                }
            }
        }
    }

    /** Whether any part of this ability mentions a library. */
    public static boolean refersToLibrary(final SpellAbility root) {
        final List<SpellAbility> todo = Lists.newArrayList(root);
        final Set<SpellAbility> seen = new java.util.HashSet<>();
        while (!todo.isEmpty()) {
            final SpellAbility sa = todo.remove(todo.size() - 1);
            if (sa == null || !seen.add(sa)) {
                continue;
            }
            if (sa.getApi() != null && LIBRARY_APIS.contains(sa.getApi())) {
                return true;
            }
            final Card host = sa.getHostCard();
            for (final String v : sa.getMapParams().values()) {
                if (mentionsLibrary(v, host)) {
                    return true;
                }
            }
            todo.add(sa.getSubAbility());
            todo.addAll(sa.getAdditionalAbilities().values());
            for (final List<? extends SpellAbility> l : sa.getAdditionalAbilityLists().values()) {
                todo.addAll(l);
            }
        }
        return false;
    }

    private static boolean mentionsLibrary(final String v, final Card host) {
        if (v == null) {
            return false;
        }
        if (v.contains("Library")) {
            return true;
        }
        return host != null && host.hasSVar(v) && host.getSVar(v).contains("Library");
    }

    /**
     * Player.getZone(Library) for a player with several: inside an ability that refers to libraries, the library
     * chosen for it - asking the first time. Never while continuous effects are being worked out.
     */
    static void referTo(final Player p) {
        final Game game = p.getGame();
        final Reference r = game.getLibraryState().references.peek();
        if (r == null || game.getAction().isCheckingStaticAbilities()) {
            return;
        }
        PlayerZone z = r.chosen.get(p);
        if (z == null || !p.getLibraryZones().contains(z)) {
            // anything looked at while choosing (the AI weighing its libraries) uses what's current
            r.chosen.put(p, p.getCurrentLibrary());
            z = choose(p, r.sa, describe(r.sa));
            r.chosen.put(p, z);
        }
        p.setCurrentLibrary(z);
    }

    private static String describe(final SpellAbility sa) {
        return sa.getHostCard() == null ? "an effect" : sa.getHostCard().getName();
    }

    /** The owner picks one of their libraries; it becomes the one "your library" means. */
    public static PlayerZone choose(final Player owner, final SpellAbility sa, final String purpose) {
        final List<PlayerZone> libs = owner.getLibraryZones();
        if (libs.size() == 1) {
            return libs.get(0);
        }
        PlayerZone z = owner.getController().chooseLibrary(libs, purpose, sa);
        if (z == null || !libs.contains(z)) {
            z = libs.get(0);
        }
        owner.setCurrentLibrary(z);
        return z;
    }

    /** Rulings: "If you draw a card, you can draw from any of your four libraries." */
    public static PlayerZone forDraw(final Player owner, final SpellAbility sa) {
        return choose(owner, sa, "draw");
    }

    /** What players are shown for one of a player's libraries. */
    public static String label(final Player owner, final PlayerZone lib, final Player viewer) {
        final List<PlayerZone> libs = owner.getLibraryZones();
        final StringBuilder sb = new StringBuilder("Library ").append(libs.indexOf(lib) + 1).append(" of ").append(libs.size())
                .append(": ").append(lib.size()).append(lib.size() == 1 ? " card" : " cards");
        if (!lib.isEmpty()) {
            final Card top = lib.get(0);
            sb.append(", top: ").append(viewer == null || top.mayPlayerLook(viewer) || top.getView().canBeShownTo(viewer.getView())
                    ? top.getName() : "hidden");
        }
        return sb.toString();
    }

    /** Split Screen enters: "shuffle your library and deal it into four libraries." */
    public static void split(final Player p, final int into, final SpellAbility sa) {
        final PlayerZone lib = p.getZone(ZoneType.Library);
        p.shuffle(sa);
        final List<List<Card>> piles = Lists.newArrayList();
        for (int i = 0; i < into; i++) {
            piles.add(Lists.newArrayList());
        }
        final List<Card> cards = Lists.newArrayList(lib.getCards());
        for (int i = 0; i < cards.size(); i++) {
            // dealt one at a time, each onto the top of its pile
            piles.get(i % into).add(0, cards.get(i));
        }
        final List<PlayerZone> zones = new java.util.ArrayList<>(List.of(lib));
        for (int i = 1; i < into; i++) {
            zones.add(new PlayerZone(ZoneType.Library, p));
        }
        for (int i = 0; i < into; i++) {
            zones.get(i).setCards(piles.get(i));
        }
        p.replaceLibrary(lib, zones);
        p.setCurrentLibrary(lib);
        p.updateLibrariesForView();
    }

    /** Split Screen leaves: "shuffle your libraries together." With just one, that's a shuffle. */
    public static void merge(final Player p, final SpellAbility sa) {
        if (p.hasSeveralLibraries()) {
            final List<PlayerZone> libs = p.getLibraryZones();
            final PlayerZone base = libs.get(0);
            final CardCollection all = new CardCollection();
            for (final PlayerZone z : libs) {
                all.addAll(z.getCards());
            }
            for (final PlayerZone z : libs) {
                if (z != base) {
                    z.setCards(CardCollection.EMPTY);
                }
            }
            java.util.Collections.shuffle(all, MyRandom.getRandom());
            base.setCards(all);
            p.mergeLibraries(base);
            for (final Card c : p.getGame().getCardsIn(ZoneType.Battlefield)) {
                if (c.getRepresentedLibrary() != null && c.getOwner().equals(p)) {
                    c.setRepresentedLibrary(base);
                }
            }
            p.updateLibrariesForView();
        }
        p.shuffle(sa);
    }

    /** The cards of the library a card's owner's "library" means for this source (the TopLibrary property). */
    public static CardCollectionView cardsFor(final Player owner, final Card source) {
        if (!owner.hasSeveralLibraries()) {
            return owner.getCardsIn(ZoneType.Library);
        }
        final Game game = owner.getGame();
        if (!game.getLibraryState().references.isEmpty() && !game.getAction().isCheckingStaticAbilities()) {
            return owner.getCardsIn(ZoneType.Library);
        }
        return libraryForStatic(owner, source).getCards();
    }

    private static String staticKey(final Card c) {
        return c.getId() + ":" + c.getGameTimestamp();
    }

    /** The library a permanent's continuous effects use for this player, or the current one if none was chosen. */
    public static PlayerZone libraryForStatic(final Player owner, final Card source) {
        if (source != null) {
            final PlayerZone z = owner.getStaticLibraries().get(staticKey(source));
            if (z != null && owner.getLibraryZones().contains(z)) {
                return z;
            }
        }
        return owner.getCurrentLibrary();
    }

    /** State-based check: each permanent whose continuous effects refer to a player's library gets one of theirs. */
    public static void chooseForStatics(final Game game) {
        for (final Player p : game.getPlayers()) {
            if (!p.hasSeveralLibraries()) {
                continue;
            }
            for (final Card c : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
                if (!staticsReferTo(c, p)) {
                    continue;
                }
                final String key = staticKey(c);
                final PlayerZone had = p.getStaticLibraries().get(key);
                if (had != null && p.getLibraryZones().contains(had)) {
                    continue;
                }
                final PlayerZone current = p.getCurrentLibrary();
                p.getStaticLibraries().put(key, choose(p, null, c.getName()));
                // choosing for a permanent isn't a reference of its own
                p.setCurrentLibrary(current);
            }
        }
    }

    private static boolean staticsReferTo(final Card c, final Player p) {
        for (final StaticAbility st : c.getStaticAbilities()) {
            if (st.hasParam("AllLibraries") || st.checkMode(StaticAbilityMode.TopLibraryPermanentsOnBattlefield)
                    || !st.zonesCheck()) {
                continue;
            }
            boolean mentions = false;
            for (final String v : st.getMapParams().values()) {
                if (mentionsLibrary(v, c)) {
                    mentions = true;
                    break;
                }
            }
            if (!mentions) {
                continue;
            }
            final String affected = st.getParamOrDefault("Affected", "");
            final Player you = c.getController();
            if (affected.contains("YouCtrl") || affected.contains("YouOwn")) {
                if (you.equals(p)) {
                    return true;
                }
            } else if (affected.contains("OppCtrl") || affected.contains("OppOwn")) {
                if (you.isOpponentOf(p)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
