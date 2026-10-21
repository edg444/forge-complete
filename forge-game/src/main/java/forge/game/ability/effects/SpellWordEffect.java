package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.NameLetters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.WordList;

/**
 * Goblin Cruciverbalist: spelling a word with the first letters of the names of permanents you control - each permanent's
 * letter once per word, spaces and punctuation skipped (rulings) - that you haven't spelled this game, for any reason.
 * A person types the word (and may consult a dictionary, as the rulings allow); the AI takes the longest common word it
 * can spell (WordList.getSpelling). The letters spelled are left in the host's SpelledLetters SVar.
 */
public class SpellWordEffect extends SpellAbilityEffect {

    /** The first letters of the names of the permanents a player controls, one per permanent. */
    public static List<Character> initials(final Player p) {
        final List<Character> letters = Lists.newArrayList();
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            final int initial = NameLetters.initial(c.getDisplayName());
            if (initial != -1) {
                letters.add((char) initial);
            }
        }
        return letters;
    }

    public static boolean canSpell(final String word, final List<Character> initials) {
        final List<Character> left = Lists.newArrayList(initials);
        for (final char ch : word.toUpperCase().toCharArray()) {
            if (!Character.isLetter(ch) || !left.remove(Character.valueOf(ch))) {
                return false;
            }
        }
        return !word.isEmpty();
    }

    /** The words a player has spelled this game, upper case. */
    public static List<String> spelled(final Player p) {
        return p.getSpelledWords();
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " spells a word.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("SpelledLetters", "Number$0");
        final List<Character> initials = initials(you);
        String word = null;
        if (you.getController().isAI()) {
            for (final String w : WordList.getSpelling(initials.size())) {
                if ((word == null || w.length() > word.length()) && canSpell(w, initials)
                        && !spelled(you).contains(w.toUpperCase())) {
                    word = w;
                }
            }
        } else {
            for (int attempt = 0; attempt < 10 && word == null; attempt++) {
                final String entered = you.getController().guessString(sa, "Spell a word you haven't spelled this game"
                        + " with the first letters of your permanents' names (" + initials + "), each once. Leave it empty"
                        + " to spell none.");
                if (entered == null || entered.trim().isEmpty()) {
                    break;
                }
                final String w = entered.trim().toUpperCase();
                if (canSpell(w, initials) && !spelled(you).contains(w)) {
                    word = w;
                }
            }
        }
        if (word == null) {
            return;
        }
        word = word.toUpperCase();
        spelled(you).add(word);
        host.setSVar("SpelledLetters", "Number$" + word.length());
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " spells " + word + ".");
        game.getAction().notifyOfValue(sa, host, you + " spells " + word, you);
    }
}
