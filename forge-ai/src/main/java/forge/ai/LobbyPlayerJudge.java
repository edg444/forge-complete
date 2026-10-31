package forge.ai;

import forge.game.Game;
import forge.game.player.Player;

/** A human's seat in the judge's game of a Judge Tower table. */
final class LobbyPlayerJudge extends LobbyPlayerAi {
    private final TableJudge link;

    LobbyPlayerJudge(final String name, final TableJudge link0) {
        super(name, null);
        link = link0;
    }

    @Override
    public Player createIngamePlayer(final Game game, final int id) {
        final Player player = new Player(getName(), game, id);
        player.setFirstController(new PlayerControllerJudge(game, player, this, link));
        return player;
    }
}
