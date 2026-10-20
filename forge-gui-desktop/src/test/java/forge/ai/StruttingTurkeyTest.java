package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Strutting Turkey (UND #14), checked against Scryfall and its 2 rulings 2026-10-20.
 */
public class StruttingTurkeyTest extends AITest {

    private Card turkeyEnters(Game game, Player p) {
        // a card put straight onto the battlefield has no active triggers until state effects run
        game.getAction().checkStateEffects(true);
        Card turkey = addCardToZone("Strutting Turkey", p, ZoneType.Hand);
        turkey = game.getAction().moveToPlay(turkey, p, null, null);
        playUntilStackClear(game);
        return game.getCardState(turkey);
    }

    private Game start() {
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void returnsACheapCreature() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        turkeyEnters(game, ai);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void combinesAnAugmentCardWithAHost() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card aug = addCardToZone("Half-Kitten, Half-", ai, ZoneType.Graveyard);
        Card turkey = turkeyEnters(game, ai);
        AssertJUnit.assertEquals("Half-Kitten, Half-Turkey", turkey.getName());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Merged).anyMatch(c -> c.getId() == aug.getId()));
        AssertJUnit.assertTrue("the Turkey is the only host", turkey.hasMergedCard());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Exile).isEmpty());
    }

    @Test
    public void manaValueThreeIsTooMuch() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card giant = addCardToZone("Hill Giant", ai, ZoneType.Graveyard);
        Card turkey = turkeyEnters(game, ai);
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertFalse(turkey.hasMergedCard());
    }
}
