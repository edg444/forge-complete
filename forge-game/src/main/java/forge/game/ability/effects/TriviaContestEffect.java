package forge.game.ability.effects;

import java.util.List;
import java.util.function.Predicate;

import forge.StaticData;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.NameLetters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;

/**
 * Trivia Contest: an opponent chooses white card, land card or planeswalker card, then mills a card; you have ten
 * seconds to name something from that category starting with the same letter as the milled card. The player names a
 * card from Forge's list of that category against the clock (honor system); the AI, knowing every card, answers in time
 * three times out of four when there's an answer to give. The AI opponent picks planeswalker card, the fewest names.
 * The host's TriviaRight SVar is 1 for a right answer.
 */
public class TriviaContestEffect extends SpellAbilityEffect {

    public static final List<String> CATEGORIES = List.of("white card", "land card", "planeswalker card");

    public static Predicate<ICardFace> inCategory(final String category) {
        return switch (category) {
            case "white card" -> f -> f.getColor().hasWhite();
            case "land card" -> f -> f.getType().isLand();
            default -> f -> f.getType().isPlaneswalker();
        };
    }

    /** Whether any card in the category has a name starting with this letter. */
    public static boolean hasAnswer(final String category, final int letter) {
        final Predicate<ICardFace> in = inCategory(category);
        for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
            for (final ICardFace f : pc.getAllFaces()) {
                if (f != null && in.test(f) && NameLetters.initial(f.getName()) == letter) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "An opponent chooses a category and mills a card; " + sa.getActivatingPlayer() + " names something from it.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("TriviaRight", "Number$0");
        if (you.getOpponents().isEmpty()) {
            return;
        }
        final Player opponent = you.getOpponents().size() == 1 ? you.getOpponents().getFirst()
                : you.getController().chooseSingleEntityForEffect(you.getOpponents(), sa, "Choose an opponent to choose", null);
        final String category = opponent.getController().isAI() ? "planeswalker card"
                : opponent.getController().chooseSomeType("category", sa, CATEGORIES);
        game.getAction().notifyOfValue(sa, opponent, category, null);

        final CardCollectionView milled = opponent.mill(1, ZoneType.Graveyard, sa, AbilityKey.newMap());
        if (milled.isEmpty()) {
            return;
        }
        final int letter = NameLetters.initial(milled.getFirst().getName());
        if (letter == -1) {
            return;
        }
        final String shown = new String(Character.toChars(letter));
        final boolean right;
        if (you.getController().isAI()) {
            right = hasAnswer(category, letter) && MyRandom.percentTrue(75);
        } else {
            final String named = you.getController().chooseCardName(sa, inCategory(category), "Card",
                    "Ten seconds! Name a " + category + " starting with " + shown);
            right = named != null && NameLetters.initial(named) == letter;
            if (named != null) {
                game.getAction().notifyOfValue(sa, you, named, null);
            }
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + (right ? " names " : " can't name ") + "a " + category
                + " starting with " + shown + " in time.");
        if (right) {
            host.setSVar("TriviaRight", "Number$1");
        }
    }
}
