package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Jiffy, Vehicle Repairer: spells ValidPlayer$ casts with jump-start aren't exiled as they leave the stack.
 */
public class StaticAbilityJumpStartNotExiled {

    public static boolean notExiled(final Player caster) {
        if (caster == null) {
            return false;
        }
        final Game game = caster.getGame();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.JumpStartNotExiled) && stAb.matchesValidParam("ValidPlayer", caster)) {
                    return true;
                }
            }
        }
        return false;
    }
}
