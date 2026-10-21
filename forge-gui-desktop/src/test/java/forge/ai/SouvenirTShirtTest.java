package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Souvenir T-Shirt (UNF #192), checked against Scryfall and its rulings 2026-10-21: two of at least two d6 results, the
 * first for power and the second for toughness.
 */
public class SouvenirTShirtTest extends AITest {

    @Test
    public void testGearUp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card shirt = addCardToZone("Souvenir T-Shirt", ai, ZoneType.Hand);
        game.getAction().moveToPlay(shirt, ai, null, AbilityKey.newMap());
        final Card worn = game.getCardState(shirt);
        final int x = Integer.parseInt(worn.getSVar("ShirtX").replace("Number$", ""));
        final int y = Integer.parseInt(worn.getSVar("ShirtY").replace("Number$", ""));
        AssertJUnit.assertTrue("d6 results", x >= 1 && x <= 6 && y >= 1 && y <= 6);
        worn.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2 + x, bears.getNetPower());
        AssertJUnit.assertEquals(2 + y, bears.getNetToughness());
    }
}
