package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreStateBasedActions {

    public static boolean ignoresStateBasedActions(final Card card) {
        return anyApplies(card.getGame(), "ValidCard", card, null);
    }

    public static boolean ignoresStateBasedActions(final Player player) {
        return anyApplies(player.getGame(), "ValidPlayer", player, null);
    }

    /** Everlasting Lich's "can't die": just rule 704.5f (0 toughness), via Rule$ ZeroToughness. */
    public static boolean ignoresZeroToughness(final Card card) {
        return ignoresStateBasedActions(card) || anyApplies(card.getGame(), "ValidCard", card, "ZeroToughness");
    }

    /** Whether the card's own abilities would exempt it from 704.5f once it's on the battlefield (AI prediction). */
    public static boolean exemptsItselfFromZeroToughness(final Card card) {
        for (final StaticAbility stAb : card.getStaticAbilities()) {
            if (stAb.checkMode(StaticAbilityMode.IgnoreStateBasedActions)
                    && "Card.Self".equals(stAb.getParam("ValidCard"))
                    && (!stAb.hasParam("Rule") || "ZeroToughness".equals(stAb.getParam("Rule")))) {
                return true;
            }
        }
        return false;
    }

    // rule null: only abilities that ignore every state-based action (no Rule param)
    private static boolean anyApplies(final Game game, final String param, final Object affected, final String rule) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.IgnoreStateBasedActions)) {
                    continue;
                }
                if (rule == null ? stAb.hasParam("Rule") : !rule.equals(stAb.getParam("Rule"))) {
                    continue;
                }
                // matchesValidParam treats a missing param as "matches everything", so a card-only
                // ability must not silently protect players too (and vice versa)
                if (stAb.hasParam(param) && stAb.matchesValidParam(param, affected)) {
                    return true;
                }
            }
        }
        return false;
    }
}
