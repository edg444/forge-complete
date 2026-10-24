package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Throne of Eldraine and Mystery Booster Playtest review (2026-10-23). */
public class ThroneOfEldraineAuditTest extends AITest {

    @Test
    public void testOpeningHandAbilityShownOnASorcery() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sidewalk = addCardToZone("Time Sidewalk", p, ZoneType.Hand);
        AssertJUnit.assertTrue(sidewalk.getAbilityText(sidewalk.getCurrentState())
                .contains("If this card is in your opening hand, you may exile it."));
    }
}
