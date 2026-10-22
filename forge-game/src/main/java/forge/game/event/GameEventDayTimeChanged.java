package forge.game.event;

// null when it stops being day or night (The Weekly Princess)
public record GameEventDayTimeChanged(Boolean daytime) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}