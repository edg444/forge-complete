package forge.game.event;

import forge.game.card.CardView;

/** Advanced Tactics: its grid (on the host's view) changed, or the host left and the grid is gone. */
public record GameEventTacticsGrid(CardView host) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) { return visitor.visit(this); }
}
