package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Dr. Julius Jumblemorph (Unstable) - Oracle text checked against Scryfall 2026-10-03; no rulings, no FAQ entry.
 */
public class DrJuliusJumblemorphTest extends AITest {

    private Card hostEnters(Game game, Player p) {
        // a card put straight onto the battlefield has no active triggers until state effects run
        game.getAction().checkStateEffects(true);
        Card kitten = addCardToZone("Adorable Kitten", p, ZoneType.Hand);
        kitten = game.getAction().moveToPlay(kitten, p, null, null);
        playUntilStackClear(game);
        return game.getCardState(kitten);
    }

    @Test
    public void testEveryCreatureTypeInEveryZone() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card inHand = addCardToZone("Dr. Julius Jumblemorph", me, ZoneType.Hand);
        Card inLibrary = addCardToZone("Dr. Julius Jumblemorph", me, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(inHand.getType().hasCreatureType("Elf"));
        AssertJUnit.assertTrue(inLibrary.getType().hasCreatureType("Squirrel"));
    }

    @Test
    public void testCombinesFromTheGraveyard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Dr. Julius Jumblemorph", ai);
        Card aug = addCardToZone("Half-Kitten, Half-", ai, ZoneType.Graveyard);
        Card kitten = hostEnters(game, ai);
        AssertJUnit.assertEquals("Half-Kitten, Half-Kitten", kitten.getName());
        AssertJUnit.assertTrue(game.getCardState(aug).isInZone(ZoneType.Merged));
    }

    @Test
    public void testCombinesFromTheLibrary() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Dr. Julius Jumblemorph", ai);
        fillLibrary(ai, 5);
        addCardToZone("Half-Kitten, Half-", ai, ZoneType.Library);
        Card kitten = hostEnters(game, ai);
        AssertJUnit.assertEquals("Half-Kitten, Half-Kitten", kitten.getName());
        AssertJUnit.assertEquals(5, ai.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testNothingToFindNothingHappens() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Dr. Julius Jumblemorph", ai);
        Card kitten = hostEnters(game, ai);
        AssertJUnit.assertEquals("Adorable Kitten", kitten.getName());
        AssertJUnit.assertFalse(kitten.hasMergedCard());
    }
}
