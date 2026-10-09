package forge.game.player;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Real hands representing tokens (Handy Dandy Clone Machine: "It must be represented by a unique hand and two fingers at
 * all times, or it ceases to exist"). Rulings and FAQ: one hand represents one token; a person has two; other people
 * may lend theirs if they agree, opponents included but not obliged; a token whose hand stops representing it ceases
 * to exist. User, 2026-10-09: track every player's two hands, ask other players to lend one (AI opponents decline, AI
 * teammates agree), then ask whether someone outside the game will (honor; the AI says no).
 */
public final class Hands {
    private Hands() { }

    public static final int PER_PERSON = 2;

    /** How many of this player's hands aren't representing a token. */
    public static int freeHands(final Player p) {
        int used = 0;
        for (final Card c : p.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.isRepresentedByHand() && c.getHandLender() == p) {
                used++;
            }
        }
        return Math.max(0, PER_PERSON - used);
    }

    /** Whether the AI could get a hand for a new token without asking anyone it can't count on. */
    public static boolean aiCanRepresent(final Player ai) {
        for (final Player p : ai.getGame().getPlayers()) {
            if (!p.isOpponentOf(ai) && freeHands(p) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a hand for a new token: its controller's own, then a lent one from another player in turn order, then
     * someone outside the game. With none, it ceases to exist. Returns whether it's represented.
     */
    public static boolean assign(final Card token, final SpellAbility sa) {
        final Player controller = token.getController();
        final Game game = controller.getGame();
        if (freeHands(controller) > 0) {
            token.setHandLender(controller);
            return true;
        }
        for (final Player p : game.getPlayersInTurnOrder(controller)) {
            if (p == controller || freeHands(p) == 0) {
                continue;
            }
            final boolean agrees = p.getController().isAI() ? !p.isOpponentOf(controller)
                    : p.getController().confirmAction(sa, PlayerActionConfirmMode.Random,
                            "Will you lend a hand to represent " + controller + "'s " + token + "?", null, token, Maps.newHashMap());
            if (agrees) {
                token.setHandLender(p);
                return true;
            }
        }
        if (!controller.getController().isAI() && controller.getController().confirmAction(sa, PlayerActionConfirmMode.Random,
                "None of the players' hands are free. Will someone outside the game lend a hand to represent " + token + "?",
                null, token, Maps.newHashMap())) {
            token.setHandLender(null);
            return true;
        }
        game.getAction().ceaseToExist(token, true);
        return false;
    }

    /** A player leaving the game takes their hands with them, and the tokens they held cease to exist. */
    public static void playerLeft(final Player p) {
        for (final Card c : new CardCollection(p.getGame().getCardsIn(ZoneType.Battlefield))) {
            if (c.isRepresentedByHand() && c.getHandLender() == p && c.getController() != p) {
                p.getGame().getAction().ceaseToExist(c, true);
            }
        }
    }
}
