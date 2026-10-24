package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;

/** Secret Lair Drop and Commander Legends review (2026-10-23). */
public class CommanderLegendsAuditTest extends AITest {

    @Test
    public void testCelestialToymakerChosenPileComesFromExile() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("The Celestial Toymaker", p);
        AssertJUnit.assertEquals("Exile", c.getSVar("DBHand").replaceAll(".*Origin\\$ (\\w+).*", "$1"));
    }

    @Test
    public void testAlharuShowsItsTokenTrigger() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Alharu, Solemn Ritualist", p);
        String text = c.getAbilityText(c.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("create a 1/1 white Spirit creature token with flying"));
    }

    @Test
    public void testPartnerVariantsReadLikeOracle() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card abby = addCard("Abby, Merciless Soldier", p);
        String text = abby.getAbilityText(abby.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("Partner—Survivors (You can have two commanders if both have this ability.)"));
        Card bear = addCard("The Bear Force Pilot Runner", p);
        text = bear.getAbilityText(bear.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("Ready to run (You can have two commanders if both have ready to run.)"));
    }

    @Test
    public void testTapXCostSaysX() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Glacian, Powerstone Engineer", p);
        String text = c.getAbilityText(c.getCurrentState());
        AssertJUnit.assertTrue(text, text.contains("{T}, Tap X untapped artifacts you control:"));
    }
}
