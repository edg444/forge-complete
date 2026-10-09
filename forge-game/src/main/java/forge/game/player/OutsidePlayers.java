package forge.game.player;

import java.util.Collection;
import java.util.function.BiFunction;
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

    /**
     * The kinds of person Kindslaver can enlist to play someone's turn (user, 2026-10-09: one of these at random) -
     * one who plays aggressively for that player, a novice who just says "go", or one who plays aggressively against
     * them.
     */
    public enum Style {
        HELPFUL("someone who plays aggressively for them"),
        NOVICE("someone who doesn't play Magic and just says \"go\""),
        SABOTEUR("someone who plays aggressively against them");

        private final String description;
        Style(final String description0) {
            description = description0;
        }
        public String getDescription() {
            return description;
        }
    }

    private static BiFunction<Player, Style, PlayerController> controllerFactory = null;

    public static void setControllerFactory(final BiFunction<Player, Style, PlayerController> factory) {
        controllerFactory = factory;
    }

    /** A person from outside the game, of the given style, to make the given player's decisions. */
    public static PlayerController newOutsideController(final Player controlled, final Style style) {
        if (controllerFactory == null) {
            throw new IllegalStateException("No factory for players from outside the game");
        }
        return controllerFactory.apply(controlled, style);
    }

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
