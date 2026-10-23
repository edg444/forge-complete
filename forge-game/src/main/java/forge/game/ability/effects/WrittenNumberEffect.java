package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

/**
 * Duelists' Convocation International. Mode$ Write: choose a 10-digit number at random (no leading zero) and write it
 * down on the permanent for all to see. Mode$ Cross: if the Defined card's mana value matches an uncrossed digit,
 * cross one out (shown as "x") and draw a card; then if all ten are crossed out, you win the game. The number goes
 * with the permanent: a new object starts with nothing written down.
 */
public class WrittenNumberEffect extends SpellAbilityEffect {

    private static final char CROSSED = 'x';

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    /** True if the card's mana value matches an uncrossed digit of the number written down on host. */
    public static boolean matchesUncrossed(final Card host, final Card c) {
        final String number = host.getWrittenDown();
        final int mv = c.getCMC();
        return number != null && mv >= 0 && mv <= 9 && number.indexOf((char) ('0' + mv)) >= 0;
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player you = sa.getActivatingPlayer();
        if ("Write".equals(sa.getParam("Mode"))) {
            final StringBuilder sb = new StringBuilder();
            sb.append((char) ('1' + MyRandom.getRandom().nextInt(9)));
            for (int i = 1; i < 10; i++) {
                sb.append((char) ('0' + MyRandom.getRandom().nextInt(10)));
            }
            host.setWrittenDown(sb.toString());
            game.getGameLog().add(GameLogEntryType.INFORMATION, host + ": " + you + " writes down " + sb + ".");
            return;
        }
        final List<Card> defined = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
        if (defined.isEmpty() || !matchesUncrossed(host, defined.get(0))) {
            return;
        }
        final char digit = (char) ('0' + defined.get(0).getCMC());
        final StringBuilder number = new StringBuilder(host.getWrittenDown());
        number.setCharAt(number.indexOf(String.valueOf(digit)), CROSSED);
        host.setWrittenDown(number.toString());
        game.getGameLog().add(GameLogEntryType.INFORMATION, host + ": " + you + " crosses out a " + digit + " (" + number + ").");
        you.drawCard();
        if (number.chars().allMatch(ch -> ch == CROSSED)) {
            you.altWinBySpellEffect(host.getName());
        }
    }
}
