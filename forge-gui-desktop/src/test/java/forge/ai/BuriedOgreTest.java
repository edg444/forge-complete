package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Buried Ogre (CMB1 #36), checked against Scryfall and rulings 2026-10-13: you may begin the game with it in your
 * graveyard, putting it there before shuffling, and lose 1 life after your starting life total is set. The AI does it
 * only when its deck can reanimate it.
 */
public class BuriedOgreTest extends AITest {

    private Player setUp(String other) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCardToZone("Buried Ogre", ai, ZoneType.Library);
        addCardToZone(other, ai, ZoneType.Library);
        fillLibrary(ai, 5);
        ai.initVariantsZones(ai.getRegisteredPlayer());
        return ai;
    }

    @Test
    public void reanimatorDeckBuriesItAndPaysALife() {
        Player ai = setUp("Reanimate");
        AssertJUnit.assertEquals(1, countCardsWithName(ai.getGame(), "Buried Ogre", ZoneType.Graveyard));
        AssertJUnit.assertEquals(19, ai.getLife());
        AssertJUnit.assertEquals("not lost during a turn", 0, ai.getLifeLostThisTurn());
    }

    @Test
    public void otherwiseItStaysInTheDeck() {
        Player ai = setUp("Lightning Bolt");
        AssertJUnit.assertEquals(1, countCardsWithName(ai.getGame(), "Buried Ogre", ZoneType.Library));
        AssertJUnit.assertEquals(20, ai.getLife());
    }

    @Test
    public void rulesTextReadsAsPrinted() {
        Player ai = setUp("Lightning Bolt");
        String text = ai.getCardsIn(ZoneType.Library).get(0).getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("You may begin the game with Buried Ogre in your graveyard. If you do, you lose 1 life."));
    }
}
