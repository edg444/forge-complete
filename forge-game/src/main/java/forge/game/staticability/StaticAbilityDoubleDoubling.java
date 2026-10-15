package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Maro's Gone Nuts: "Double any effect that doubles." What a doubling effect multiplies by: 2, then 4 with one out, 8
 * with two, 16 with three (its rulings). Doubling effects ask for it wherever they double - MultiplyCounter's default,
 * Pump's {@code Double}, ReplaceToken's default, ReplaceMana's {@code ReplaceAmount$ 2}, and the {@code Doubled} /
 * {@code DoubledExtra} math the scripts that double use in place of {@code Twice}.
 */
public class StaticAbilityDoubleDoubling {

    public static int factor(final Game game) {
        if (game == null) {
            return 2;
        }
        int n = 0;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.DoubleDoubling)) {
                    n++;
                }
            }
        }
        return 1 << Math.min(30, 1 + n);
    }
}
