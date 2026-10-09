package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

/** A player joined a game already in progress (Better Than One), so the match screen needs a place for them. */
public record GameEventPlayerAdded(PlayerView player, PlayerView seatedAfter) implements GameEvent {

    public GameEventPlayerAdded(final Player player, final Player seatedAfter) {
        this(PlayerView.get(player), PlayerView.get(seatedAfter));
    }

    @Override
    public <T> T visit(final IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return player + " joined the game";
    }
}
