package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * The Trivia Mastermind: its controller says the name of the card (Defined$), then chooses a category among power,
 * toughness, or mana value; an opponent guesses that number about the card, then the card is revealed. If they guessed
 * wrong, the card is put into its controller's hand. A card without power and toughness offers only mana value.
 */
public class TriviaGuessEffect extends SpellAbilityEffect {

    public static final String POWER = "Power";
    public static final String TOUGHNESS = "Toughness";
    public static final String MANA_VALUE = "Mana value";

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player p = sa.getActivatingPlayer();
        final List<Card> defined = getDefinedCardsOrTargeted(sa);
        if (defined.isEmpty() || p.getOpponents().isEmpty()) {
            return;
        }
        final Card card = defined.get(0);

        final List<String> categories = new ArrayList<>();
        if (card.isCreature() || card.getCurrentState().hasPrintedPT()) {
            categories.add(POWER);
            categories.add(TOUGHNESS);
        }
        categories.add(MANA_VALUE);
        final String category = p.getController().isAI() ? Aggregates.random(categories)
                : p.getController().chooseSomeType("category", sa, categories);
        if (category == null) {
            return;
        }
        game.getAction().notifyOfValue(sa, p, card.getName() + " - guess its " + category.toLowerCase(), null);

        final Player guesser = p.getOpponents().size() == 1 ? p.getOpponents().get(0)
                : p.getController().chooseSingleEntityForEffect(p.getOpponents(), sa, "Choose an opponent to guess", null);
        sa.setSVar("TriviaCategory", category);
        final int actual = POWER.equals(category) ? card.getNetPower()
                : TOUGHNESS.equals(category) ? card.getNetToughness() : card.getCMC();
        // The Fact Checker: the guesser just chooses how it comes out
        final Boolean outcome = forge.game.staticability.StaticAbilityChooseGuessOutcome.chosenOutcome(guesser, sa);
        final int guess = outcome != null ? (outcome ? actual : actual + 1)
                : guesser.getController().chooseNumber(sa, "Guess the " + category.toLowerCase() + " of " + card.getName(), 0, 20);

        game.getAction().reveal(new CardCollection(card), p, true);
        game.getGameLog().add(GameLogEntryType.INFORMATION, guesser + " guesses " + guess + " for the "
                + category.toLowerCase() + " of " + card.getName() + "; it's " + actual + ".");
        if (guess != actual) {
            game.getAction().moveToHand(card, sa, AbilityKey.newMap());
        }
    }
}
