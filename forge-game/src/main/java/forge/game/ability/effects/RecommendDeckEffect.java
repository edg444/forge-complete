package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Commander Recommender: its controller brings out another Commander deck - one of their saved ones or a random
 * precon (user ruling 2026-10-22; the AI's is always a precon) - and fifteen random cards other than the commander are
 * revealed. Each opponent guesses the commander from a list of it and other commanders of its colors (an AI guesses
 * at random), then it's revealed; a wrong guess is a "guessed wrong" for triggers.
 */
public class RecommendDeckEffect extends SpellAbilityEffect {

    /** Where the decks come from; the GUI sets it. */
    public interface DeckSource {
        List<Deck> commanderDecks();
        Deck randomPrecon();
    }
    private static DeckSource source = null;
    public static void setDeckSource(final DeckSource s) {
        source = s;
    }

    private static final String PRECON = "A random Commander precon";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Bring out another Commander deck; each opponent guesses its commander.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        if (source == null) {
            return;
        }
        Deck deck = null;
        if (!p.getController().isAI()) {
            final String current = p.getRegisteredPlayer() == null ? null : p.getRegisteredPlayer().getDeck().getName();
            final Map<String, Deck> byName = new LinkedHashMap<>();
            for (final Deck d : source.commanderDecks()) {
                if (d.has(DeckSection.Commander) && !d.get(DeckSection.Commander).isEmpty() && !d.getName().equals(current)) {
                    byName.putIfAbsent(d.getName(), d);
                }
            }
            final List<String> names = new ArrayList<>(byName.keySet());
            names.add(PRECON);
            final String pick = p.getController().chooseSomeType("Commander deck", sa, names);
            deck = pick == null || PRECON.equals(pick) ? null : byName.get(pick);
        }
        if (deck == null) {
            deck = source.randomPrecon();
        }
        if (deck == null || !deck.has(DeckSection.Commander) || deck.get(DeckSection.Commander).isEmpty()) {
            return;
        }
        final List<PaperCard> commanders = deck.get(DeckSection.Commander).toFlatList();
        final List<PaperCard> rest = new ArrayList<>(deck.getMain().toFlatList());
        Collections.shuffle(rest, MyRandom.getRandom());
        final List<String> revealed = new ArrayList<>();
        for (final PaperCard pc : rest.subList(0, Math.min(15, rest.size()))) {
            revealed.add(pc.getName());
        }
        game.getAction().notifyOfValue(sa, p, "Fifteen cards from the deck: " + String.join(", ", revealed), null);
        game.getGameLog().add(GameLogEntryType.INFORMATION, p + " reveals fifteen cards of another Commander deck: "
                + String.join(", ", revealed) + ".");

        final Set<String> answers = new LinkedHashSet<>();
        ColorSet identity = ColorSet.C;
        for (final PaperCard c : commanders) {
            answers.add(c.getName());
            identity = ColorSet.fromMask(identity.getColor() | c.getRules().getColorIdentity().getColor());
        }
        final ColorSet colors = identity;
        final List<String> others = new ArrayList<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
            if (pc.getRules().canBeCommander() && !answers.contains(pc.getName())
                    && pc.getRules().getColorIdentity().getColor() == colors.getColor()) {
                others.add(pc.getName());
            }
        }
        Collections.shuffle(others, MyRandom.getRandom());
        final List<String> choices = new ArrayList<>(answers);
        choices.addAll(others.subList(0, Math.min(9, others.size())));
        Collections.sort(choices);

        for (final Player opp : p.getOpponents()) {
            final String guess = opp.getController().isAI() ? Aggregates.random(choices)
                    : opp.getController().chooseSomeType("guess for the deck's commander", sa, choices);
            final boolean right = guess != null && answers.contains(guess);
            game.getGameLog().add(GameLogEntryType.INFORMATION, opp + " guesses " + guess + (right ? " - right." : " - wrong."));
            if (!right) {
                GuessedWrongEffect.fire(opp);
            }
        }
        game.getAction().notifyOfValue(sa, p, "The deck's commander: " + String.join(" and ", answers), null);
        game.getGameLog().add(GameLogEntryType.INFORMATION, "The deck's commander is " + String.join(" and ", answers) + ".");
    }
}
