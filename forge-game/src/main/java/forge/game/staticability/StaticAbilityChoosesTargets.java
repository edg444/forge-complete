package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Gleemax - somebody other than the caster chooses the targets, for every spell and ability in the
 * game rather than for one named ability.
 */
public class StaticAbilityChoosesTargets {

    /**
     * Who picks targets for this ability. Normally the activator; a static can take that over.
     */
    public static Player getChooser(final Player activator, final SpellAbility sa) {
        if (activator == null) {
            return activator;
        }
        final Game game = activator.getGame();
        if (game == null) {
            return activator;
        }

        Player chooser = activator;
        long bestTimestamp = Long.MIN_VALUE;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.ChoosesTargets)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidSA", sa) || !stAb.matchesValidParam("ValidActivator", activator)) {
                    continue;
                }
                // two of these in play contradict each other, so the newest wins, the way any pair of
                // conflicting continuous effects would
                if (stAb.getHostCard().getGameTimestamp() >= bestTimestamp) {
                    bestTimestamp = stAb.getHostCard().getGameTimestamp();
                    chooser = stAb.getHostCard().getController();
                }
            }
        }
        return chooser;
    }
}
