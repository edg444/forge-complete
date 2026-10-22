package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Actualizer: its controller reads out the name, mana cost, type line, power, and toughness of a card in their
 * hand, lying about one of them; each opponent guesses which one is the lie (all guesses are in before the card is
 * revealed), and each who guessed wrong loses 3 life. The card is the host's chosen card, for the discard-and-draw.
 * The lies to choose from are another real card's name, cost or type line, or a different number (my call, so the
 * reading sounds like a real card; there's no free-text input).
 */
public class ActualizeEffect extends SpellAbilityEffect {

    private static final List<String> PARTS = List.of("Name", "Mana cost", "Type line", "Power", "Toughness");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " reads out a card in their hand, lying about one thing.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final CardCollection hand = new CardCollection(p.getCardsIn(ZoneType.Hand));
        if (hand.isEmpty()) {
            return;
        }
        final boolean ai = p.getController().isAI();
        final Card card = ai ? Aggregates.random(hand)
                : p.getController().chooseSingleEntityForEffect(hand, sa, "Choose a card in your hand to read out", null);
        if (card == null) {
            return;
        }
        final Map<String, String> truth = new LinkedHashMap<>();
        truth.put("Name", card.getName());
        truth.put("Mana cost", card.getManaCost().isNoCost() ? "(none)" : card.getManaCost().toString());
        truth.put("Type line", card.getType().toString());
        truth.put("Power", card.isCreature() ? String.valueOf(card.getNetPower()) : "(none)");
        truth.put("Toughness", card.isCreature() ? String.valueOf(card.getNetToughness()) : "(none)");

        final String lied = ai ? Aggregates.random(PARTS) : p.getController().chooseSomeType("thing to lie about", sa, PARTS);
        if (lied == null) {
            return;
        }
        final List<String> lies = lies(lied, truth.get(lied));
        final String lie = ai || lies.size() == 1 ? Aggregates.random(lies)
                : p.getController().chooseSomeType("lie", sa, lies);
        final Map<String, String> said = new LinkedHashMap<>(truth);
        said.put(lied, lie == null ? lies.get(0) : lie);

        final StringBuilder reading = new StringBuilder();
        for (final Map.Entry<String, String> e : said.entrySet()) {
            reading.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        game.getAction().notifyOfValue(sa, p, reading.toString().trim(), null);
        game.getGameLog().add(GameLogEntryType.INFORMATION, p + " reads out a card: " + reading.toString().trim().replace("\n", "; ") + ".");

        final Map<Player, String> guesses = new LinkedHashMap<>();
        for (final Player opp : p.getOpponents()) {
            guesses.put(opp, opp.getController().isAI() ? Aggregates.random(PARTS)
                    : opp.getController().chooseSomeType("lie (which one is incorrect?)", sa, PARTS));
        }
        game.getAction().reveal(new CardCollection(card), p, true);
        game.getGameLog().add(GameLogEntryType.INFORMATION, "The lie was the " + lied.toLowerCase() + ".");
        for (final Map.Entry<Player, String> g : guesses.entrySet()) {
            if (!lied.equals(g.getValue())) {
                g.getKey().loseLife(3, false, false, sa);
            }
        }
        host.setChosenCards(new CardCollection(card));
    }

    private static List<String> lies(final String part, final String actual) {
        final Set<String> out = new LinkedHashSet<>();
        if ("Power".equals(part) || "Toughness".equals(part)) {
            for (int i = 0; i <= 12; i++) {
                if (!String.valueOf(i).equals(actual)) {
                    out.add(String.valueOf(i));
                }
            }
            return new ArrayList<>(out);
        }
        final List<PaperCard> pool = new ArrayList<>(StaticData.instance().getCommonCards().getUniqueCards());
        for (int tries = 0; out.size() < 8 && tries < 200; tries++) {
            final PaperCard pc = pool.get(MyRandom.getRandom().nextInt(pool.size()));
            final String value = switch (part) {
                case "Name" -> pc.getName();
                case "Mana cost" -> pc.getRules().getManaCost().isNoCost() ? "(none)" : pc.getRules().getManaCost().toString();
                default -> pc.getRules().getType().toString();
            };
            if (!value.equals(actual)) {
                out.add(value);
            }
        }
        return new ArrayList<>(out);
    }
}
