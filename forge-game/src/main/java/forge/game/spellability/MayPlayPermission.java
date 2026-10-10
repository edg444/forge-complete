package forge.game.spellability;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.event.GameEventAddLog;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;

/**
 * Princess Luna's "your friends may cast them with your permission": a MayPlay static with
 * {@code MayPlayWithPermission$ <players>} asks those players before anyone else plays the card through it.
 * A refusal holds for the rest of the turn, so a turned-down player isn't offered the card again until then.
 */
public final class MayPlayPermission {
    private MayPlayPermission() {
    }

    public static boolean ask(final SpellAbility sa) {
        final StaticAbility st = sa.getMayPlay();
        if (st == null || !st.hasParam("MayPlayWithPermission")) {
            return true;
        }
        final Player asker = sa.getActivatingPlayer();
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        for (final Player granter : AbilityUtils.getDefinedPlayers(st.getHostCard(), st.getParam("MayPlayWithPermission"), st)) {
            if (granter.equals(asker)) {
                continue;
            }
            final String logic = "MayPlayPermission:" + (granter.isOpponentOf(asker) ? "opponent" : "ally");
            final String message = asker + " asks your permission to cast " + card.getDisplayName() + ". Allow it?";
            if (!granter.getController().confirmStaticApplication(card, null, message, logic)) {
                card.refusePlayPermission(asker, game.getPhaseHandler().getTurn());
                game.fireEvent(new GameEventAddLog(GameLogEntryType.INFORMATION,
                        granter + " didn't let " + asker + " cast " + card.getDisplayName() + ".", card));
                game.getAction().checkStaticAbilities();
                return false;
            }
        }
        return true;
    }
}
