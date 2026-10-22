package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Fact Checker: "If you would guess a card, instead just choose the outcome you want." Guess effects about a card
 * ask {@link #chosenOutcome} first: null means guess as usual, otherwise it's whether the guess comes out right. The
 * AI always wants to be right.
 */
public class StaticAbilityChooseGuessOutcome {

    public static boolean applies(final Player player) {
        final Game game = player.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.ChooseGuessOutcome) && stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Boolean chosenOutcome(final Player guesser, final SpellAbility sa) {
        if (guesser == null || !applies(guesser)) {
            return null;
        }
        if (guesser.getController().isAI()) {
            return true;
        }
        return guesser.getController().confirmAction(sa, null,
                "Instead of guessing, choose the outcome you want: guess right? (No = guess wrong.)", null);
    }
}
