package forge.game.zone;

import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.game.card.Card;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;

/**
 * Dandan (Forgetful Fish): one library or graveyard that is every player's. Each sharing player's zone map holds this
 * same object, so "your library" is it for all of them. getPlayer() is the first sharer, where the cards are counted
 * once when going over every player's zones.
 */
public class SharedPlayerZone extends PlayerZone {
    private static final long serialVersionUID = 1L;

    private final List<Player> sharers;

    public SharedPlayerZone(final ZoneType zone, final List<Player> sharers0) {
        super(zone, sharers0.get(0));
        sharers = ImmutableList.copyOf(sharers0);
    }

    public List<Player> getSharers() {
        return sharers;
    }

    @Override
    public boolean belongsTo(final Player player) {
        return sharers.contains(player);
    }

    @Override
    protected void onChanged() {
        for (final Player p : sharers) {
            p.updateZoneForView(this);
        }
    }

    @Override
    protected void fireZoneEvent(final EventValueChangeType type, final Card c) {
        // every sharer's view of it changed
        for (final Player p : sharers) {
            game.fireEvent(new GameEventZone(getZoneType(), p, type, c));
        }
    }

    @Override
    public String toString() {
        return "the shared " + getZoneType().toString().toLowerCase();
    }
}
