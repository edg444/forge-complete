package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Evil Presents - a creature that attacks the very player controlling it.
 * <p>
 * Nothing in the normal rules allows this: CR 506.2 makes the attacking player's own creatures
 * unable to attack them, and it is enforced in two separate places - the attacking player is never
 * listed as a possible defender, and MustAttack deliberately skips any entity whose turn it is. Both
 * have to make an exception for a creature carrying this static, so it is one mode covering both
 * halves of "always attacks its controller": it *may* attack its controller, and it *must*.
 */
public class StaticAbilityAttacksItsController {

    /** Whether this creature attacks the player who controls it. */
    public static boolean qualifies(final Card attacker) {
        if (attacker == null || attacker.getGame() == null) {
            return false;
        }
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttacksItsController)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidCreature", attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether this player has to be offered to their own creatures as a defender. */
    public static boolean anyControlledBy(final Player player) {
        if (player == null) {
            return false;
        }
        for (final Card c : player.getCreaturesInPlay()) {
            if (qualifies(c)) {
                return true;
            }
        }
        return false;
    }
}
