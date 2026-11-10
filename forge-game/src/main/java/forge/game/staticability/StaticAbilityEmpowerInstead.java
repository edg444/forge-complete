package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Remnants of Beleren: "If you would empower Jace, you may instead put that many loyalty counters on a Jace you
 * control." ValidPlayer$ is who empowers, Type$ the empowered type, ValidCard$ what may take the counters. Fork-only.
 */
public class StaticAbilityEmpowerInstead {

    /** The permanents this player may put the empower counters on instead; empty when nothing lets them. */
    public static CardCollection candidates(final Player p, final String type) {
        final CardCollection out = new CardCollection();
        final Game game = p.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.EmpowerInstead)
                        || !stAb.matchesValidParam("ValidPlayer", p)
                        || (stAb.hasParam("Type") && !stAb.getParam("Type").equalsIgnoreCase(type))) {
                    continue;
                }
                for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
                    if (stAb.matchesValidParam("ValidCard", c) && !out.contains(c)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }
}
