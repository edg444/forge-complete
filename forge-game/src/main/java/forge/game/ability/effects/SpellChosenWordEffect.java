package forge.game.ability.effects;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Spelling Bee: a player (Chooser$) looks at the top card of your library and chooses a word on it - from its name, type
 * line or text box, real word or not (rulings) - and you spell it. Spelling happens out loud, so you're shown the word
 * and say whether you got it right (user, 2026-10-21: honor system); the AI gets it right by chance, less often the
 * longer the word. The host's SpelledCorrectly SVar says how it went.
 */
public class SpellChosenWordEffect extends SpellAbilityEffect {

    /** The words on a card a player could choose: its name, type line and text box, flavor text included. */
    public static List<String> wordsOn(final Card c) {
        final Set<String> seen = Sets.newHashSet();
        final List<String> words = Lists.newArrayList();
        final String text = c.getName() + " " + c.getType().toString() + " " + c.getOracleText() + " " + c.getFlavorText();
        for (final String raw : text.split("[^\\p{L}'’-]+")) {
            final String word = raw.replaceAll("^['’-]+|['’-]+$", "");
            if (word.chars().anyMatch(Character::isLetter) && seen.add(word.toLowerCase())) {
                words.add(word);
            }
        }
        return words;
    }

    /** How likely the AI is to spell a word right: short ones nearly always, long ones about half the time. */
    public static int aiSpellingChance(final String word) {
        return Math.max(40, Math.min(97, 109 - 6 * word.length()));
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " spells a word from the top card of their library.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        host.setSVar("SpelledCorrectly", "Number$0");
        final CardCollection top = new CardCollection(you.getCardsIn(ZoneType.Library).stream().limit(1).toList());
        if (top.isEmpty()) {
            return;
        }
        final List<Player> choosers = sa.hasParam("Chooser")
                ? AbilityUtils.getDefinedPlayers(host, sa.getParam("Chooser"), sa) : List.of(you);
        final Player chooser = choosers.isEmpty() ? you : choosers.get(0);
        final List<String> words = wordsOn(top.getFirst());
        if (words.isEmpty()) {
            return;
        }
        chooser.getController().reveal(top, ZoneType.Library, you, "Top card of " + you + "'s library: ", false);
        final String word = chooser.getController().chooseStringForEffect(words, sa,
                "Choose a word on this card for " + you + " to spell");
        if (word == null) {
            return;
        }
        you.getGame().getAction().notifyOfValue(sa, chooser, "the word \"" + word + "\"", null);

        final boolean correct;
        if (you.getController().isAI()) {
            correct = MyRandom.percentTrue(aiSpellingChance(word));
        } else {
            correct = you.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose,
                    chooser + " chose \"" + word + "\". Spell it out loud without looking at it. Did you spell it correctly?",
                    ImmutableList.of("Correct", "Wrong"), null, null);
        }
        you.getGame().getGameLog().add(forge.game.GameLogEntryType.INFORMATION,
                you + " spells \"" + word + "\" " + (correct ? "correctly." : "incorrectly."));
        if (correct) {
            host.setSVar("SpelledCorrectly", "Number$1");
        }
    }
}
