package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Multiplayer Nitpickers: "Treat your effects that scale off of 'players' or 'opponents' as though there were one
 * more." User ruling 2026-10-22: player and opponent counts are one higher, and effects repeated for each player or
 * each opponent run once more for a phantom one who does nothing (myriad's extra token attacks nobody; Braids' phantom
 * doesn't sacrifice, so you draw).
 */
public class StaticAbilityPhantomPlayer {

    public static boolean applies(final Player player) {
        if (player == null) {
            return false;
        }
        final Game game = player.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.PhantomPlayer) && stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a defined set of players (RepeatPlayers$, ForEach$, ...) is "each player" or "each opponent". */
    public static boolean scalesOffPlayers(final String defined) {
        return defined != null && (defined.equals("Player") || defined.contains("Opp"));
    }
}
