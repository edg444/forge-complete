package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Portal Three Kingdoms script review and R&D's Secret Lair printed text (2026-10-23). */
public class PortalThreeKingdomsAuditTest extends AITest {

    private int dragonTriggersFromHand(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card dragon = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Zodiac Dragon", "PTK"), p);
        p.getZone(ZoneType.Hand).add(dragon);
        game.getAction().checkStateEffects(true);
        game.getAction().moveToGraveyard(dragon, null);
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        return game.getStack().size();
    }

    @Test
    public void testZodiacDragonReturnsWhenDiscardedAsPrinted() {
        AssertJUnit.assertEquals(1, dragonTriggersFromHand(true));
    }

    @Test
    public void testZodiacDragonOnlyFromBattlefieldByOracle() {
        AssertJUnit.assertEquals(0, dragonTriggersFromHand(false));
    }
}
