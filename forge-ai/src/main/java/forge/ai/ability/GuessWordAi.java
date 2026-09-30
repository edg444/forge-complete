package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.GuessWordEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.WordList;

/**
 * Hangman. The AI plays it the way a person does: from what's on the sheet, never from the noted word itself
 * (unless it noted the word). It matches the pattern against the word list and guesses the letter most of the
 * matching words have, or the word once only one matches; with nothing matching it falls back on the commonest
 * letters in English.
 *
 * With its own Hangman it makes an opponent guess while most of the word is still hidden - a wrong guess is a
 * +1/+1 counter. Against someone else's it only pays when its guess will finish the word.
 */
public class GuessWordAi extends SpellAbilityAi {
    private static final String BY_FREQUENCY = "ETAOINSRHLDCUMFPGWYBVKXJQZ";

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!host.hasGuessableWord()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        if (host.getController().equals(ai)) {
            long hidden = host.getWordPattern().chars().filter(c -> c == '_').count();
            if (hidden < 3 || sa.getActivationsThisTurn() > 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            final boolean oppEndStep = !ph.isPlayerTurn(ai) && ph.is(PhaseType.END_OF_TURN);
            final boolean ownMain2 = ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2);
            if (!oppEndStep && !ownMain2) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
            // anyone but whoever noted the word, who'd just say it
            for (final Player opp : ai.getOpponents()) {
                if (!opp.equals(host.getWordNoter()) && sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        if (!host.getController().isOpponentOf(ai) || !sa.canTarget(ai) || !knowsTheWord(ai, host)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(ai);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** Words from the list that fit what the sheet shows: the pattern, the wrong letters, the wrong words. */
    public static List<String> candidates(final Card host) {
        final String pattern = host.getWordPattern();
        final String guessed = host.getGuessedLetters();
        final List<String> result = new ArrayList<>();
        for (final String word : WordList.get(pattern.length(), pattern.length())) {
            if (host.getWrongWordGuesses().contains(word)) {
                continue;
            }
            boolean fits = true;
            for (int i = 0; i < pattern.length() && fits; i++) {
                final char p = pattern.charAt(i);
                final char w = word.charAt(i);
                // a blank can't be a letter that's already been guessed: right ones are all filled in
                fits = p == '_' ? guessed.indexOf(w) < 0 : p == w;
            }
            if (fits) {
                result.add(word);
            }
        }
        return result;
    }

    private static boolean knowsTheWord(final Player ai, final Card host) {
        return ai.equals(host.getWordNoter()) || host.isWordFullyGuessed() || candidates(host).size() == 1;
    }

    /** What the AI says when it guesses the whole word. */
    public static String guessWord(final Player ai, final Card host) {
        if (ai.equals(host.getWordNoter())) {
            return host.getChosenType();
        }
        if (host.isWordFullyGuessed()) {
            return host.getWordPattern();
        }
        final List<String> candidates = candidates(host);
        return candidates.isEmpty() ? "" : candidates.get(0);
    }

    @Override
    public String chooseString(final Player ai, final SpellAbility sa, final List<String> options) {
        final Card host = sa.getHostCard();
        if (knowsTheWord(ai, host) && options.contains(GuessWordEffect.GUESS_THE_WORD)) {
            return GuessWordEffect.GUESS_THE_WORD;
        }
        final List<String> candidates = candidates(host);
        String best = null;
        int bestCount = 0;
        for (final String letter : options) {
            if (letter.length() != 1) {
                continue;
            }
            int count = 0;
            for (final String word : candidates) {
                if (word.contains(letter)) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                best = letter;
            }
        }
        if (best != null) {
            return best;
        }
        for (int i = 0; i < BY_FREQUENCY.length(); i++) {
            final String letter = BY_FREQUENCY.substring(i, i + 1);
            if (options.contains(letter)) {
                return letter;
            }
        }
        return options.isEmpty() ? null : options.get(0);
    }
}
