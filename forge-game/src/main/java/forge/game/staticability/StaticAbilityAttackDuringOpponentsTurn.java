package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * Party Crasher - "You can attack with this creature once each combat during each opponent's turn."
 * <p>
 * Only the active player declares attackers (CR 508.1a), so this is a second declaration inside their declare
 * attackers step: once they're done, the creature's controller may declare it attacking any of their own opponents
 * (rulings; user, 2026-10-02, for multiplayer). PhaseHandler.declareAttackersTurnBasedAction runs it.
 */
public class StaticAbilityAttackDuringOpponentsTurn {

    /** Whether this creature can attack during an opponent's turn. */
    public static boolean qualifies(final Card attacker) {
        if (attacker == null || attacker.getGame() == null) {
            return false;
        }
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackDuringOpponentsTurn)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidCreature", attacker)) {
                    return true;
                }
            }
        }
        return false;
    }
}
