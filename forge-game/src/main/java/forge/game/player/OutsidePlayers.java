package forge.game.player;

import java.util.Collection;
import java.util.function.Function;

import forge.LobbyPlayer;

/**
 * Where a person from outside the game comes from when a card brings one in (Better Than One). The user ruled such a
 * person is played by the AI, which lives in forge-ai and so can't be built from here; the GUI layer plugs in a
 * factory at startup, as it does for {@link forge.game.card.CardThreat}. The factory is handed the names already in
 * the game so it can pick one that isn't.
 */
public final class OutsidePlayers {
    private OutsidePlayers() { }

    private static Function<Collection<String>, LobbyPlayer> aiFactory = null;

    public static void setAiFactory(final Function<Collection<String>, LobbyPlayer> factory) {
        aiFactory = factory;
    }

    public static boolean isAvailable() {
        return aiFactory != null;
    }

    /** An AI-played lobby player named unlike anyone in usedNames; it also creates its in-game player. */
    public static LobbyPlayer newAiPlayer(final Collection<String> usedNames) {
        if (aiFactory == null) {
            throw new IllegalStateException("No factory for players from outside the game");
        }
        return aiFactory.apply(usedNames);
    }
}
