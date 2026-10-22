package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

/**
 * The Pleasant Taxer: doing Action$ costs ValidPlayer$ Amount$ more. Search is searching a library; Draw is drawing a
 * card beyond the first each turn. Those cost nothing of their own, so the player pays the tax each time or the search
 * or draw doesn't happen (user ruling 2026-10-22). Every instance adds up.
 */
public class StaticAbilityActionTax {

    /** Whether the player may go ahead: there's no tax, or they paid it. The AI pays whenever it can. */
    public static boolean payFor(final Player player, final String action) {
        final Game game = player.getGame();
        int total = 0;
        Card host = null;
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.ActionTax) && action.equals(stAb.getParam("Action"))
                        && stAb.matchesValidParam("ValidPlayer", player)) {
                    total += AbilityUtils.calculateAmount(source, stAb.getParamOrDefault("Amount", "1"), stAb);
                    host = source;
                }
            }
        }
        if (total <= 0) {
            return true;
        }
        final SpellAbility sa = new SpellAbility.EmptySa(ApiType.Pump, host, player);
        sa.putParam("UnlessAI", "Chance.100");
        final boolean paid = player.getController().payCostToPreventEffect(new Cost(String.valueOf(total), false), sa, false,
                new FCollection<>(player));
        if (!paid) {
            game.getGameLog().add(forge.game.GameLogEntryType.INFORMATION, player + " didn't pay {" + total + "} for "
                    + ("Search".equals(action) ? "searching a library" : "drawing a card") + ", so it doesn't happen.");
        }
        return paid;
    }
}
