package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

/**
 * Hangman: "Target player who doesn't control this creature guesses the noted word or an unguessed letter in that
 * word. If they guess wrong, ..." The word was noted with ChooseType's Guessable$. Forge keeps the hangman sheet:
 * it fills in each instance of a right letter, won't offer a letter twice (Unstable rulings), and runs the
 * WordGuessed trigger once the word or all of its letters have been guessed.
 */
public class GuessWordEffect extends SpellAbilityEffect {
    public static final String GUESS_THE_WORD = "Guess the word";

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }
        return sa.getHostCard().getName() + " - guess the noted word or a letter in it.";
    }

    /** The letters nobody has guessed yet, A to Z. */
    public static List<String> unguessedLetters(final Card host) {
        final List<String> letters = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            if (host.getGuessedLetters().indexOf(c) < 0) {
                letters.add(String.valueOf(c));
            }
        }
        return letters;
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        if (!host.hasGuessableWord()) {
            return;
        }
        final List<Player> guessers = getDefinedPlayersOrTargeted(sa, "Defined");
        if (guessers.isEmpty()) {
            return;
        }
        final Player guesser = guessers.get(0);
        final String word = host.getChosenType();

        final List<String> options = new ArrayList<>();
        options.add(GUESS_THE_WORD);
        options.addAll(unguessedLetters(host));
        final String sheet = host.getName() + ": " + host.getGuessableWordDisplay();
        String choice = guesser.getController().chooseStringForEffect(options, sa, sheet + " - guess a letter or the word");
        if (choice == null || !options.contains(choice)) {
            choice = options.get(options.size() > 1 ? 1 : 0);
        }

        final boolean correct;
        final String said;
        boolean wordGuessed = false;
        if (choice.equals(GUESS_THE_WORD)) {
            final String guess = guesser.getController().guessString(sa, sheet + "\nGuess the word");
            final String typed = guess == null ? "" : guess.trim();
            correct = typed.equalsIgnoreCase(word);
            wordGuessed = correct;
            said = typed.isEmpty() ? "(no guess)" : typed.toUpperCase();
            if (correct) {
                host.revealGuessableWord();
            } else if (!typed.isEmpty()) {
                host.addWrongWordGuess(typed);
            }
        } else {
            final char letter = choice.charAt(0);
            correct = word.indexOf(letter) >= 0;
            said = choice;
            host.addGuessedLetter(letter);
        }

        game.getAction().notifyOfValue(sa, guesser,
                said + (correct ? " - right.  " : " - wrong.  ") + host.getGuessableWordDisplay(), null);

        if (correct && sa.hasParam("GuessCorrect")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessCorrect"));
        } else if (!correct && sa.hasParam("GuessWrong")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessWrong"));
        }

        // only the guess that finishes the word: a wrong letter afterwards isn't guessing it again
        if (wordGuessed || (correct && host.isWordFullyGuessed())) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(host);
            runParams.put(AbilityKey.Player, guesser);
            game.getTriggerHandler().runTrigger(TriggerType.WordGuessed, runParams, false);
        }
    }
}
