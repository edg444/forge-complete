package forge.game.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.game.trigger.Trigger;

public class ExtraPhase {
    private final PhaseType phase;
    private List<Trigger> delTrig = Collections.synchronizedList(new ArrayList<>());

    public ExtraPhase(PhaseType phase) {
        this.phase = phase;
    }

    public PhaseType getPhase() {
        return phase;
    }

    // Throat Wolf: a combat phase that belongs to another player, in which only one creature can attack
    private forge.game.player.Player owner = null;
    private forge.game.card.Card onlyAttacker = null;
    public void setOwner(final forge.game.player.Player owner, final forge.game.card.Card onlyAttacker) {
        this.owner = owner;
        this.onlyAttacker = onlyAttacker;
    }
    public forge.game.player.Player getOwner() {
        return owner;
    }
    public forge.game.card.Card getOnlyAttacker() {
        return onlyAttacker;
    }

    public void addTrigger(Trigger deltrigger) {
        this.delTrig.add(deltrigger);
    }

    public List<Trigger> getDelayedTriggers() {
        return delTrig;
    }

}
