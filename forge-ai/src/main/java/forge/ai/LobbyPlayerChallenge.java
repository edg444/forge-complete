package forge.ai;

import forge.game.Game;
import forge.game.player.Player;

/** The seat of a Hero's Path challenge deck, played by {@link PlayerControllerChallenge}. */
public class LobbyPlayerChallenge extends LobbyPlayerAi {

    public LobbyPlayerChallenge(final String name) {
        super(name, null);
    }

    @Override
    public Player createIngamePlayer(final Game game, final int id) {
        final Player p = new Player(getName(), game, id);
        p.setFirstController(new PlayerControllerChallenge(game, p, this));
        return p;
    }
}
