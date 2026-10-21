package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Poison Tolerance N (Melira's Snacks): it takes N additional poison counters for ValidPlayer$ to lose the game to
 * poison. Every instance adds up.
 */
public class StaticAbilityPoisonTolerance {

    public static int additionalCounters(final Player player) {
        final Game game = player.getGame();
        int total = 0;
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.PoisonTolerance) && stAb.matchesValidParam("ValidPlayer", player)) {
                    total += AbilityUtils.calculateAmount(source, stAb.getParamOrDefault("Amount", "1"), stAb);
                }
            }
        }
        return total;
    }
}
