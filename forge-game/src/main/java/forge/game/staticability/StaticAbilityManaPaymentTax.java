package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Asker of More Mana: each time ValidPlayer$ pays mana - for a spell, an ability, an unless cost, an attack tax,
 * anything - they pay Amount$ more generic mana. Every instance adds up.
 */
public class StaticAbilityManaPaymentTax {

    public static int additionalMana(final Player payer) {
        final Game game = payer.getGame();
        int total = 0;
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.ManaPaymentTax) && stAb.matchesValidParam("ValidPlayer", payer)) {
                    total += AbilityUtils.calculateAmount(source, stAb.getParamOrDefault("Amount", "1"), stAb);
                }
            }
        }
        return total;
    }
}
