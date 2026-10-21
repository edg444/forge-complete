package forge.game.cost;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * Greatest Show in the Multiverse: exile N instant and/or sorcery cards you own with the same artist and different names
 * from outside the game. In an Un- game any such cards will do, not only a deck's sideboard (rulings; user, 2026-10-21:
 * any card in Forge's database), so it can always be paid. A card with several artists counts for each of them
 * (rulings). The cards come into the game in exile, exiled with the spell.
 */
public class CostExileFromOutside extends CostPart {
    private static final long serialVersionUID = 1L;

    private static Map<String, List<PaperCard>> byArtist = null;

    public CostExileFromOutside(final String amount) {
        super(amount, "Instant;Sorcery", "instant and/or sorcery cards with the same artist and different names");
    }

    /** Each artist with at least min instant and sorcery cards of different names, and one printing of each name. */
    public static synchronized Map<String, List<PaperCard>> artists(final int min) {
        if (byArtist == null) {
            final Map<String, Map<String, PaperCard>> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (final PaperCard pc : StaticData.instance().getCommonCards().getAllCards()) {
                if (pc.getRules() == null || !(pc.getRules().getType().isInstant() || pc.getRules().getType().isSorcery())
                        || pc.getArtist() == null) {
                    continue;
                }
                for (final String artist : pc.getArtist().split("\\s*&\\s*")) {
                    if (!artist.isBlank()) {
                        names.computeIfAbsent(artist.trim(), k -> new TreeMap<>()).putIfAbsent(pc.getName(), pc);
                    }
                }
            }
            byArtist = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (final Map.Entry<String, Map<String, PaperCard>> e : names.entrySet()) {
                byArtist.put(e.getKey(), Lists.newArrayList(e.getValue().values()));
            }
        }
        final Map<String, List<PaperCard>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (final Map.Entry<String, List<PaperCard>> e : byArtist.entrySet()) {
            if (e.getValue().size() >= min) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public int paymentOrder() {
        return 25;
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return true;
    }

    @Override
    public final String toString() {
        return "Exile " + getAmount() + " " + getTypeDescription() + " you own from outside the game";
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision decision, final SpellAbility ability, final boolean effect) {
        final Card host = ability.getHostCard();
        for (final Card c : decision.cards) {
            payer.getZone(ZoneType.Exile).add(c);
            c.setExiledWith(host);
            c.setExiledBy(payer);
            host.addExiledCard(c);
        }
        return true;
    }

    @Override
    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
