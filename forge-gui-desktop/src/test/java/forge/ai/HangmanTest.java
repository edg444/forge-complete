package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.GuessWordAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.GuessWordEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.WordList;

/**
 * Hangman (Unstable): "As this creature enters, secretly note a word with six to eight letters. {1}: Target player
 * who doesn't control this creature guesses the noted word or an unguessed letter in that word. If they guess
 * wrong, put a +1/+1 counter on this creature. Any player may activate this ability. When a player guesses the
 * noted word or all of its letters, sacrifice this creature." - Oracle text checked against Scryfall 2026-09-30.
 */
public class HangmanTest extends AITest {

    private static SpellAbility guessAbility(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.GuessWord) {
                return sa;
            }
        }
        return null;
    }

    /** Has the opponent guess once, outside the stack. */
    private static void guess(Card hangman, Player activator, Player guesser) {
        SpellAbility sa = guessAbility(hangman);
        sa.setActivatingPlayer(activator);
        sa.resetTargets();
        sa.getTargets().add(guesser);
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testWordList() {
        initAndCreateGame();
        List<String> words = WordList.get(6, 8);
        AssertJUnit.assertTrue(words.size() > 400);
        for (String word : words) {
            AssertJUnit.assertTrue(word, word.length() >= 6 && word.length() <= 8 && WordList.isPlainWord(word));
        }
        AssertJUnit.assertTrue(WordList.get(9, 20).isEmpty());
        AssertJUnit.assertFalse(WordList.isPlainWord("don't"));
        AssertJUnit.assertFalse(WordList.isPlainWord("two words"));
    }

    @Test
    public void testNotesAWordNobodySees() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card hangman = addCardToZone("Hangman", me, ZoneType.Hand);
        hangman = game.getAction().moveToPlay(hangman, me, null, null);

        AssertJUnit.assertTrue(hangman.hasGuessableWord());
        String word = hangman.getChosenType();
        AssertJUnit.assertTrue(word, WordList.get(6, 8).contains(word));
        AssertJUnit.assertEquals(me, hangman.getWordNoter());
        // the view only has the blanks, which also tell everyone how long the word is
        AssertJUnit.assertEquals(String.join(" ", "_".repeat(word.length()).split("")), hangman.getView().getChosenType());
        AssertJUnit.assertEquals("word", hangman.getView().getChosenTypeKind());
    }

    @Test
    public void testTheAiDoesNotNoteTheSameWordEveryTime() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        java.util.Set<String> noted = new java.util.HashSet<>();
        for (int i = 0; i < 12; i++) {
            Card hangman = addCardToZone("Hangman", me, ZoneType.Hand);
            noted.add(game.getAction().moveToPlay(hangman, me, null, null).getChosenType());
        }
        AssertJUnit.assertTrue(noted.toString(), noted.size() > 3);
    }

    @Test
    public void testWhoMayActivateAndWhoGuesses() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hangman = addCard("Hangman", me);
        hangman.setGuessableWord("castle", me);
        SpellAbility sa = guessAbility(hangman);
        AssertJUnit.assertEquals("{1}: Target player who doesn't control this creature guesses the noted word or an "
                + "unguessed letter in that word. If they guess wrong, put a +1/+1 counter on this creature. "
                + "Any player may activate this ability.", sa.getDescription().trim());

        // every Oracle sentence; Forge lists triggers ahead of activated abilities
        AssertJUnit.assertEquals("As this creature enters, secretly note a word with six to eight letters. | "
                + "When a player guesses the noted word or all of its letters, sacrifice this creature. | "
                + sa.getDescription().trim(), String.join(" | ", hangman.getAbilityText().trim().split("[\r\n]+")));

        for (Player activator : new Player[] {me, opp}) {
            sa.setActivatingPlayer(activator);
            AssertJUnit.assertTrue(sa.getRestrictions().checkActivatorRestrictions(hangman, sa));
            AssertJUnit.assertTrue(sa.canTarget(opp));
            AssertJUnit.assertFalse(sa.canTarget(me));
        }
    }

    @Test
    public void testWrongLettersGrowItAndTheLastRightOneEndsIt() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hangman = addCard("Hangman", me);
        // not in the word list, so with B and N showing the AI has nothing to match and goes by letter frequency
        // registers the new card's trigger, as any priority would
        game.getAction().checkStateEffects(true);
        hangman.setGuessableWord("banana", me);
        hangman.addGuessedLetter('B');
        hangman.addGuessedLetter('N');
        AssertJUnit.assertEquals("B_N_N_", hangman.getWordPattern());
        AssertJUnit.assertTrue(GuessWordAi.candidates(hangman).isEmpty());
        AssertJUnit.assertFalse(GuessWordEffect.unguessedLetters(hangman).contains("B"));

        guess(hangman, me, opp); // E
        AssertJUnit.assertEquals(1, hangman.getCounters(CounterEnumType.P1P1));
        guess(hangman, opp, opp); // T
        AssertJUnit.assertEquals(2, hangman.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("BNET", hangman.getGuessedLetters());
        AssertJUnit.assertEquals("B _ N _ N _ - wrong: E, T", hangman.getView().getChosenType());
        AssertJUnit.assertTrue(game.getStack().isEmpty());

        guess(hangman, me, opp); // A, every instance of it
        AssertJUnit.assertEquals("BANANA", hangman.getWordPattern());
        AssertJUnit.assertEquals(2, hangman.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(hangman.isInZone(ZoneType.Battlefield));
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(hangman).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testGuessingTheWord() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hangman = addCard("Hangman", me);
        // registers the new card's trigger, as any priority would
        game.getAction().checkStateEffects(true);
        hangman.setGuessableWord("castle", me);
        for (char c : "ASTLE".toCharArray()) {
            hangman.addGuessedLetter(c);
        }
        // one word fits _ASTLE
        AssertJUnit.assertEquals(List.of("CASTLE"), GuessWordAi.candidates(hangman));
        guess(hangman, opp, opp);
        AssertJUnit.assertEquals(0, hangman.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("C A S T L E", hangman.getView().getChosenType());
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(hangman).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAWrongWordIsNotGuessedTwice() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hangman = addCard("Hangman", me);
        // a person's word the list doesn't have, that looks like one it does
        hangman.setGuessableWord("hastle", me);
        for (char c : "ASTLE".toCharArray()) {
            hangman.addGuessedLetter(c);
        }
        guess(hangman, me, opp);
        AssertJUnit.assertEquals(1, hangman.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(List.of("CASTLE"), hangman.getWrongWordGuesses());
        AssertJUnit.assertEquals("_ A S T L E - wrong: CASTLE", hangman.getView().getChosenType());
        AssertJUnit.assertTrue(GuessWordAi.candidates(hangman).isEmpty());

        // now it's letters, commonest first: O, I, N, R, then H
        for (int i = 0; i < 4; i++) {
            guess(hangman, me, opp);
        }
        AssertJUnit.assertEquals(5, hangman.getCounters(CounterEnumType.P1P1));
        guess(hangman, me, opp);
        AssertJUnit.assertTrue(hangman.isWordFullyGuessed());
    }

    @Test
    public void testTheAiGuessesFromTheSheetNotTheWord() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hangman = addCard("Hangman", me);
        hangman.setGuessableWord("castle", me);
        Card other = addCard("Hangman", me);
        other.setGuessableWord("wizard", me);
        // same sheet, same guess - whatever the words are
        SpellAbility sa = guessAbility(hangman);
        List<String> options = GuessWordEffect.unguessedLetters(hangman);
        String first = new GuessWordAi().chooseString(opp, sa, options);
        AssertJUnit.assertEquals(first, new GuessWordAi().chooseString(opp, guessAbility(other), options));
        AssertJUnit.assertEquals(1, first.length());

        // whoever noted the word knows it, if the Hangman ends up with someone else
        other.setController(opp, game.getNextTimestamp());
        AssertJUnit.assertEquals("WIZARD", GuessWordAi.guessWord(me, other));
    }

    @Test
    public void testWhenTheAiPays() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCard("Hangman", ai);
        mine.setGuessableWord("castle", ai);
        SpellAbility sa = guessAbility(mine);
        sa.setActivatingPlayer(ai);

        // its own: in its second main phase, while the word is mostly hidden, and aimed at the opponent
        moveToMain2(game, ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        moveToMain2(game, ai);
        for (char c : "CAST".toCharArray()) {
            mine.addGuessedLetter(c);
        }
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());

        // someone else's: only once it can finish the word
        Card theirs = addCard("Hangman", opp);
        theirs.setGuessableWord("dragon", opp);
        SpellAbility theirSa = guessAbility(theirs);
        theirSa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(theirSa).canPlayWithSubs(ai, theirSa).willingToPlay());
        for (char c : "RAGON".toCharArray()) {
            theirs.addGuessedLetter(c);
        }
        AssertJUnit.assertEquals(List.of("DRAGON"), GuessWordAi.candidates(theirs));
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(theirSa).canPlayWithSubs(ai, theirSa).willingToPlay());
        AssertJUnit.assertEquals(ai, theirSa.getTargets().getFirstTargetedPlayer());
    }
}
