package forge.game;

import java.util.Map;

import forge.card.GamePieceType;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Form of the Approach of the Second Sun: a player who is a card balances the cards above them in their library on
 * their head. A human reports a fall themselves (the card's honor-system action). The AI has no head, so per the
 * user's ruling its stack wobbles: each time cards go on top of it there's a chance they fall, growing with the stack.
 */
public final class HeadBalance {
    private HeadBalance() { }

    /** Chance of a fall per card on the head, each time the stack grows. */
    public static final int PERCENT_PER_CARD = 2;
    public static final int MAX_PERCENT = 50;

    /** The card its owner has become, in their library, or null. */
    public static Card personCard(final Player p) {
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            if (c.getGamePieceType() == GamePieceType.PERSON) {
                return c;
            }
        }
        return null;
    }

    /** The cards balanced on a player's head: every card above them in their library. */
    public static int cardsOnHead(final Player p) {
        final Card self = personCard(p);
        return self == null ? 0 : p.getCardsIn(ZoneType.Library).indexOf(self);
    }

    /** Called after a card goes into a library: if that put more cards on its owner's head, an AI's stack may fall. */
    public static void cardsMayHaveGoneOnTop(final Player owner) {
        final Card self = personCard(owner);
        if (self == null) {
            return;
        }
        final int now = cardsOnHead(owner);
        final int before = self.getCardsOnHead();
        self.setCardsOnHead(now);
        if (now > before && owner.getController().isAI()) {
            wobble(owner, now);
        }
    }

    /** An AI's stack of {@code onHead} cards just grew: roll to see if it falls. */
    public static void wobble(final Player owner, final int onHead) {
        if (onHead > 0 && MyRandom.getRandom().nextInt(100) < Math.min(MAX_PERCENT, PERCENT_PER_CARD * onHead)) {
            cardsFell(owner);
        }
    }

    /** One or more cards fell off a player's head. */
    public static void cardsFell(final Player owner) {
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(owner);
        owner.getGame().getTriggerHandler().runTrigger(TriggerType.CardsFellOffHead, runParams, false);
    }
}
