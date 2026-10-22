package forge.game.staticability;

import java.util.LinkedHashSet;
import java.util.Set;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Slobad, Actually Just Fine: "You can sacrifice creatures and artifacts interchangeably." Whenever ValidPlayer$ is
 * asked to sacrifice a creature, an artifact will do, and the other way around.
 */
public class StaticAbilitySacrificeInterchangeably {

    public static boolean applies(final Player player) {
        final Game game = player.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.SacrificeInterchangeably) && stAb.matchesValidParam("ValidPlayer", player)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The valid string with each Creature or Artifact alternative also allowing the other type. */
    public static String expand(final String valid, final Player player, final String separator) {
        if (valid == null || player == null || !applies(player)) {
            return valid;
        }
        final Set<String> parts = new LinkedHashSet<>();
        for (final String part : valid.split(separator)) {
            parts.add(part);
            if (part.matches("Creature($|[.+].*)")) {
                parts.add("Artifact" + part.substring(8));
            } else if (part.matches("Artifact($|[.+].*)")) {
                parts.add("Creature" + part.substring(8));
            }
        }
        return String.join(separator, parts);
    }
}
