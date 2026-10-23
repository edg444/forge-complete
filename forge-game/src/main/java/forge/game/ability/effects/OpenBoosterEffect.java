package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.StaticData;
import forge.card.CardEdition;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.BoosterGenerator;

/**
 * Stocking Tiger. The printed card has you open a real sealed booster you own from outside the game,
 * so this generates a genuine one from the chosen set's own booster template rather than faking a
 * handful of cards - the same generator draft and sealed use.
 */
public class OpenBoosterEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("Container")) {
            return sa.getHostCard() + " - you may reveal a container of booster packs and open one of them at random.";
        }
        final String set = sa.getHostCard().getChosenExpansion();
        return sa.getHostCard() + " - open the chosen booster pack"
                + (set.isEmpty() ? "" : " (" + set + ")") + ".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        if (sa.hasParam("Container")) {
            openFromContainer(sa);
            return;
        }
        if (sa.hasParam("Swap")) {
            openAndSwap(sa);
            return;
        }
        if (sa.hasParam("PutUnder")) {
            // The Chaos Keeper: a sealed pack of its controller's choice goes under it, unopened
            final String code = chooseSet(sa, sa.getActivatingPlayer());
            sa.getHostCard().getPackUnder().clear();
            if (code != null) {
                sa.getHostCard().getPackUnder().addAll(openPack(code));
            }
            return;
        }
        if (sa.hasParam("TakeFromUnder")) {
            takeFromUnder(sa);
            return;
        }
        if (sa.hasParam("Draft")) {
            draftPack(sa);
            return;
        }
        if (sa.hasParam("ToBoosterPile")) {
            openToBoosterPile(sa);
            return;
        }
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String setCode = host.getChosenExpansion();
        final List<PaperCard> pack = openPack(setCode);
        if (pack.isEmpty()) {
            return;
        }

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final CardCollection opened = new CardCollection();
            for (final PaperCard pc : pack) {
                final Card card = Card.fromPaperCard(pc, p);
                final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
                moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
                // Opening Ceremony keeps the pack in exile to cast from (Destination$ Exile, RememberOpened$)
                final ZoneType to = ZoneType.smartValueOf(sa.getParamOrDefault("Destination", "Hand"));
                final Card moved = game.getAction().moveTo(to, card, sa, moveParams);
                opened.add(moved);
                if (sa.hasParam("RememberOpened")) {
                    host.addRemembered(moved);
                }
            }
            if (!opened.isEmpty() && sa.hasParam("Reveal")) {
                game.getAction().reveal(opened, p, false,
                        p + " opens a " + setCode + " booster pack: ");
            }
        }
    }

    /**
     * A Container of Booster Packs: the player may reveal a container of Container$ different sealed packs - naming their
     * sets, honor system; the AI's holds sets at random - then one at random is opened, and they put a card from it into
     * their hand. The rest of the pack stays out of the game.
     */
    private static void openFromContainer(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int size = Integer.parseInt(sa.getParam("Container"));
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            // shown by name with the code, chosen by code
            final Map<String, String> byLabel = new java.util.LinkedHashMap<>();
            for (final String code : boosterSetCodes()) {
                final CardEdition ed = StaticData.instance().getEditions().get(code);
                byLabel.put((ed == null ? code : ed.getName()) + " (" + code + ")", code);
            }
            final List<String> sets = new java.util.ArrayList<>(byLabel.keySet());
            if (sets.size() < size || !p.getController().confirmAction(sa, null,
                    "Reveal a container holding at least " + size + " different sealed booster packs?", null)) {
                continue;
            }
            final List<String> container = new java.util.ArrayList<>();
            while (container.size() < size) {
                final String set = p.getController().isAI() ? forge.util.Aggregates.random(sets)
                        : p.getController().chooseSomeType("Magic set", sa, sets);
                if (set == null) {
                    break;
                }
                container.add(byLabel.get(set));
                sets.remove(set);
            }
            if (container.size() < size) {
                continue;
            }
            final String setCode = forge.util.Aggregates.random(container);
            final CardCollection pack = new CardCollection();
            for (final PaperCard pc : openPack(setCode)) {
                pack.add(Card.fromPaperCard(pc, p));
            }
            game.getAction().notifyOfValue(sa, p, "the " + setCode + " pack, from " + String.join(", ", container), null);
            if (pack.isEmpty()) {
                continue;
            }
            final Card kept = p.getController().chooseSingleEntityForEffect(pack, sa,
                    "Put a card from the " + setCode + " pack into your hand", false, null);
            if (kept != null) {
                game.getAction().moveTo(ZoneType.Hand, kept, sa, AbilityKey.newMap());
            }
        }
    }

    /**
     * The Beleaguered Boxer: the player opens a pack of a set they pick (the AI's is random), removes up to Swap$ cards
     * in their hand or graveyard from their deck for good - exiled for the rest of the match - and puts that many cards
     * from the pack into their hand, now part of the deck (Match.swapInDeck). The rest of the pack stays out of the game.
     */
    private static void openAndSwap(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int max = Integer.parseInt(sa.getParam("Swap"));
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final Map<String, String> byLabel = new java.util.LinkedHashMap<>();
            for (final String code : boosterSetCodes()) {
                final CardEdition ed = StaticData.instance().getEditions().get(code);
                byLabel.put((ed == null ? code : ed.getName()) + " (" + code + ")", code);
            }
            if (byLabel.isEmpty()) {
                continue;
            }
            final List<String> labels = new java.util.ArrayList<>(byLabel.keySet());
            final String label = p.getController().isAI() ? forge.util.Aggregates.random(labels)
                    : p.getController().chooseSomeType("Magic set", sa, labels);
            if (label == null) {
                continue;
            }
            final String setCode = byLabel.get(label);
            final CardCollection pack = new CardCollection();
            for (final PaperCard pc : openPack(setCode)) {
                pack.add(Card.fromPaperCard(pc, p));
            }
            if (pack.isEmpty()) {
                continue;
            }
            final List<String> names = new java.util.ArrayList<>();
            pack.forEach(c -> names.add(c.getName()));
            game.getAction().notifyOfValue(sa, p, p + " opens a " + setCode + " booster pack: " + String.join(", ", names), null);

            final CardCollection removable = new CardCollection();
            for (final Card c : p.getCardsIn(java.util.EnumSet.of(ZoneType.Hand, ZoneType.Graveyard))) {
                if (c.getOwner().equals(p) && !c.isToken() && c.getPaperCard() != null) {
                    removable.add(c);
                }
            }
            final CardCollection removed = new CardCollection(p.getController().chooseCardsForEffect(removable, sa,
                    "Remove up to " + max + " cards in your hand or graveyard from your deck permanently", 0,
                    Math.min(max, Math.min(removable.size(), pack.size())), true, null));
            final List<Card> taken = new java.util.ArrayList<>();
            if (!removed.isEmpty()) {
                taken.addAll(p.getController().chooseCardsForEffect(pack, sa,
                        "Add " + removed.size() + " cards from the pack to your hand", removed.size(), removed.size(), false, null));
            }
            for (int i = 0; i < removed.size() && i < taken.size(); i++) {
                final Card out = removed.get(i);
                final Card in = taken.get(i);
                if (in.getPaperCard() instanceof PaperCard added) {
                    game.getMatch().swapInDeck(out, added);
                }
                game.getAction().exile(out, sa, AbilityKey.newMap());
                game.getAction().moveTo(ZoneType.Hand, in, sa, AbilityKey.newMap());
            }
        }
    }

    /** A set to open a pack of: the player's pick, or a random one for the AI (user ruling 2026-10-22). */
    private static String chooseSet(final SpellAbility sa, final Player p) {
        final Map<String, String> byLabel = new java.util.LinkedHashMap<>();
        List<String> codes = boosterSetCodes();
        if (sa.hasParam("Sets")) {
            final List<String> allowed = java.util.Arrays.asList(sa.getParam("Sets").split(","));
            codes = allowed.stream().filter(codes::contains).toList();
        }
        for (final String code : codes) {
            final CardEdition ed = StaticData.instance().getEditions().get(code);
            byLabel.put((ed == null ? code : ed.getName()) + " (" + code + ")", code);
        }
        if (byLabel.isEmpty()) {
            return null;
        }
        final List<String> labels = new java.util.ArrayList<>(byLabel.keySet());
        final String label = p.getController().isAI() ? forge.util.Aggregates.random(labels)
                : p.getController().chooseSomeType("Magic set", sa, labels);
        return label == null ? null : byLabel.get(label);
    }

    /**
     * Heroes of Kamigawa: "Open up to one sealed Kamigawa booster pack and shuffle those cards into your booster pile." The
     * player picks one of Sets$ (the AI at random, user ruling 2026-10-22); the AI always opens one.
     */
    private static void openToBoosterPile(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        if (sa.hasParam("UpToOne") && !p.getController().isAI()
                && !p.getController().confirmAction(sa, null, "Open a sealed booster pack?", null)) {
            return;
        }
        final String code = chooseSet(sa, p);
        final List<PaperCard> pack = openPack(code);
        if (pack.isEmpty()) {
            return;
        }
        final forge.game.zone.PlayerZone pile = p.getZone(ZoneType.BoosterPile);
        for (final PaperCard pc : pack) {
            pile.add(Card.fromPaperCard(pc, p));
        }
        pile.shuffle();
        p.getGame().getGameLog().add(forge.game.GameLogEntryType.INFORMATION,
                p + " opens a " + code + " booster pack and shuffles its " + pack.size() + " cards into their booster pile.");
    }

    /** The Chaos Keeper: the pack under it is revealed and its controller puts one of its cards into their hand. */
    private static void takeFromUnder(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player p = sa.getActivatingPlayer();
        final List<PaperCard> under = host.getPackUnder();
        if (under.isEmpty()) {
            return;
        }
        final CardCollection shown = new CardCollection();
        final Map<Card, PaperCard> paperOf = new java.util.HashMap<>();
        for (final PaperCard pc : under) {
            final Card c = Card.fromPaperCard(pc, p);
            shown.add(c);
            paperOf.put(c, pc);
        }
        final List<String> names = new java.util.ArrayList<>();
        shown.forEach(c -> names.add(c.getName()));
        game.getAction().notifyOfValue(sa, p, "Under " + host + ": " + String.join(", ", names), null);
        final Card kept = p.getController().chooseSingleEntityForEffect(shown, sa, "Put one of them into your hand", false, null);
        if (kept != null) {
            under.remove(paperOf.get(kept));
            game.getAction().moveTo(ZoneType.Hand, kept, sa, AbilityKey.newMap());
        }
    }

    /**
     * The Charitable Drafter: a pack of a set its controller picks is drafted face up, its controller first and then in
     * turn order, until it's gone; each pick goes into its drafter's hand and is theirs for the game.
     */
    private static void draftPack(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player first = sa.getActivatingPlayer();
        final String code = chooseSet(sa, first);
        if (code == null) {
            return;
        }
        final List<PaperCard> pack = new java.util.ArrayList<>(openPack(code));
        final List<Player> order = new java.util.ArrayList<>(game.getPlayersInTurnOrder(first));
        int turn = 0;
        while (!pack.isEmpty() && !order.isEmpty()) {
            final Player drafter = order.get(turn % order.size());
            turn++;
            final CardCollection shown = new CardCollection();
            final Map<Card, PaperCard> paperOf = new java.util.HashMap<>();
            for (final PaperCard pc : pack) {
                final Card c = Card.fromPaperCard(pc, drafter);
                shown.add(c);
                paperOf.put(c, pc);
            }
            final Card pick = drafter.getController().chooseSingleEntityForEffect(shown, sa,
                    "Draft a card from the " + code + " pack", false, null);
            if (pick == null) {
                continue;
            }
            pack.remove(paperOf.get(pick));
            game.getGameLog().add(forge.game.GameLogEntryType.INFORMATION, drafter + " drafts " + pick.getName() + ".");
            game.getAction().moveTo(ZoneType.Hand, pick, sa, AbilityKey.newMap());
        }
    }

    /** Codes of the sets that can produce a booster pack, in order. */
    public static List<String> boosterSetCodes() {
        final Set<String> sets = Sets.newTreeSet();
        final StaticData data = StaticData.instance();
        if (data != null && data.getEditions() != null) {
            for (final CardEdition e : data.getEditions().getOrderedEditions()) {
                if (!e.getCode().isEmpty() && e.getBoosterTemplate() != null) {
                    sets.add(e.getCode());
                }
            }
        }
        return List.copyOf(sets);
    }

    /** A freshly generated booster pack of the set, or an empty list when the set can't produce one. */
    public static List<PaperCard> openPack(final String setCode) {
        if (setCode == null || setCode.isEmpty()) {
            return List.of();
        }
        final StaticData data = StaticData.instance();
        if (data == null || data.getEditions() == null) {
            return List.of();
        }
        final CardEdition edition = data.getEditions().get(setCode);
        final SealedTemplate template = edition == null ? null : edition.getBoosterTemplate();
        if (template == null) {
            return List.of();
        }
        try {
            final List<PaperCard> pack = BoosterGenerator.getBoosterPack(template);
            return pack == null ? List.of() : pack;
        } catch (final Exception e) {
            // a set can be listed with a booster template it can't actually fill
            return List.of();
        }
    }
}
