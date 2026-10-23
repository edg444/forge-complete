package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Planeshift R&D's Secret Lair printed text (2026-10-23). */
public class PlaneshiftAuditTest extends AITest {

    @Test
    public void testLordOfTheUndeadPumpsItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card lord = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Lord of the Undead", "PLS"), p);
        p.getZone(ZoneType.Battlefield).add(lord);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, lord.getNetPower());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, lord.getNetPower());
    }
}
