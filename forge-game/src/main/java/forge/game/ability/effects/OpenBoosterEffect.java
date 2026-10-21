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
