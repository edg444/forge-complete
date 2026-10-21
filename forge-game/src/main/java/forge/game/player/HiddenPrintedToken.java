package forge.game.player;

import forge.game.GameEntity;
import forge.game.card.Card;

/**
 * Surprise Party: a printed token its owner hid on the battlefield - the table - before it's anything in the game. It
 * isn't a game object until it's put onto the battlefield; it's behind one of its owner's permanents or in plain sight
 * (one per spot, since the tokens can't touch), and an opponent who points at its spot has spotted it.
 */
public class HiddenPrintedToken {
    private final Player owner;
    private final GameEntity spot;
    private final String tokenScript;
    private boolean spotted = false;

    public HiddenPrintedToken(final Player owner, final GameEntity spot, final String tokenScript) {
        this.owner = owner;
        this.spot = spot;
        this.tokenScript = tokenScript;
    }

    /** Where it is: what it was behind leaving the battlefield (or its owner's control) leaves it in plain sight. */
    public GameEntity getSpot() {
        if (spot instanceof Card c && (!c.isInPlay() || c.getController() != owner)) {
            return owner;
        }
        return spot;
    }

    public String getTokenScript() {
        return tokenScript;
    }

    public boolean isSpotted() {
        return spotted;
    }

    public void setSpotted() {
        spotted = true;
    }
}
