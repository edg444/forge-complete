package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class BecomeMonarchEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(Lang.joinHomogenous(tgtPlayers)).append(tgtPlayers.size() == 1 ? " becomes" : " become");
        sb.append(" the monarch.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // TODO: improve ai and fix corner cases
        final String set = sa.getOriginalHost().getSetCode();

        // Fioran Reformist: "get rid of the monarchy" - no one is the monarch
        if (sa.hasParam("Abolish")) {
            final forge.game.Game game = sa.getHostCard().getGame();
            if (game.getMonarch() != null) {
                game.getMonarch().removeMonarchEffect();
                game.setMonarch(null);
            }
            return;
        }

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            if (p.canBecomeMonarch()) {
                p.getGame().getAction().becomeMonarch(p, set);
            }
        }
    }

}
