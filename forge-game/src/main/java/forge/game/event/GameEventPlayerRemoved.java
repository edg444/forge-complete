package forge.game.event;

import forge.game.player.PlayerView;

/**
 * Something that had a place on the match screen no longer needs it: Interplanar Brushwagg's interplanar battlefield
 * once nothing is left on it, taken off the screen the way a defeated player in a multiplayer game is.
 */
public record GameEventPlayerRemoved(PlayerView player) implements GameEvent {

    @Override
    public <T> T visit(final IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return player + " left the screen";
    }
}
