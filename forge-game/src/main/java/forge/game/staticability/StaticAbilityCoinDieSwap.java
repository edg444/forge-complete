package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Pokey, the Scallywagg: "If you would flip a coin, you may instead roll a d20" (CoinFlipAsD20) and "If you would roll a
 * d20, you may instead flip a coin" (D20AsCoinFlip), for ValidPlayer$. Each swap counts as the other kind of event
 * entirely, and the swapped event isn't swapped back (FlipCoinEffect, RollDiceEffect).
 */
public class StaticAbilityCoinDieSwap {

    // set while a swapped flip or roll happens, so it isn't swapped again
    private static final ThreadLocal<Boolean> SWAPPING = ThreadLocal.withInitial(() -> false);

    public static boolean isSwapping() {
        return SWAPPING.get();
    }
    public static void setSwapping(final boolean b) {
        SWAPPING.set(b);
    }

    public static boolean applies(final Player player, final StaticAbilityMode mode) {
        if (SWAPPING.get()) {
            return false;
        }
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(mode) && stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }
}
