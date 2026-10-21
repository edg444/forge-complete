package forge.game.event;

import forge.game.card.CardView;
import java.util.List;

public record GameEventFlipOntoBattlefield(CardView flipped, CardView target, List<CardView> hit, int timesFlipped, boolean finished, TossStyle style) implements GameEvent {
    // fork: the funny cards that throw pieces, dice or themselves in place pass a TossStyle
    public GameEventFlipOntoBattlefield(CardView flipped, CardView target, List<CardView> hit, int timesFlipped, boolean finished) {
        this(flipped, target, hit, timesFlipped, finished, TossStyle.CARD);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) { return visitor.visit(this); }
}
