package forge.game.mulligan;

import forge.game.GameLogEntryType;
import forge.game.event.GameEventAddLog;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public abstract class AbstractMulligan {
    Player player;
    int timesMulliganed = 0;
    boolean firstMulliganFree = false;
    boolean kept = false;

    public AbstractMulligan(Player p, boolean firstMullFree) {
        player = p;
        firstMulliganFree = firstMullFree;
    }

    public Player getPlayer() { return player; }

    public void beforeFirstMulligan() {}
    public abstract boolean canMulligan();
    public abstract int handSizeAfterNextMulligan();

    public int tuckCardsDuringMulligan() {
        return 0;
    }

    public void mulligan() {
        if (!returnHand()) return;
        player.shuffle(null);
        timesMulliganed++;
        mulliganDraw();
        player.onMulliganned();
    }

    /** Puts the hand into the library, not yet shuffled. False when there's no hand to put back. */
    public boolean returnHand() {
        CardCollection toMulligan = new CardCollection(player.getCardsIn(ZoneType.Hand));
        if (toMulligan.isEmpty()) return false;
        // a card that started the game in hand (Rift) isn't shuffled back
        toMulligan.removeIf(player::isOpeningHandStarter);
        revealPreMulligan(toMulligan);
        for (final Card c : toMulligan) {
            player.getGame().getAction().moveToLibrary(c, null);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /** A mulligan that counts (Dandan's free mulligans don't). */
    public void countMulligan() {
        timesMulliganed++;
    }

    public int getTimesMulliganed() {
        return timesMulliganed;
    }

    /**
     * Dandan: a seven-card hand with fewer than two lands or fewer than two spells may be revealed for a free
     * mulligan, until the player has taken a regular one (Secret Lair Dandan Deck rules, 2026-03-15).
     */
    public boolean canTakeFreeMulligan() {
        if (timesMulliganed > 0 || !canMulligan()) {
            return false;
        }
        final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        final int lands = CardLists.count(hand, Card::isLand);
        return lands < 2 || hand.size() - lands < 2;
    }

    public void keep() {
        kept = true;
    }

    public boolean hasKept() {
        return kept;
    }

    public void revealPreMulligan(CardCollection toMulligan) {}

    public void mulliganDraw() {
        player.drawCards(cardsToDrawAfterMulligan());
        afterMulliganDraw();
    }

    public int cardsToDrawAfterMulligan() {
        return Math.max(0, handSizeAfterNextMulligan() - player.getOpeningHandStartersInHand());
    }

    /** Once the new hand is drawn (or dealt). */
    public void afterMulliganDraw() {}

    public void afterMulligan() {
        player.getGame().fireEvent(new GameEventAddLog(GameLogEntryType.MULLIGAN, Localizer.getInstance().getMessage("lblPlayerKeepNCardsHand", player.getName(), String.valueOf(player.getZone(ZoneType.Hand).size()))));
    }
}