package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Spy Eye: drawing a card from another player's library - still your draw, still their card. */
public class SpyEyeTest extends AITest {

    private Card onTop(String name, Player p) {
        Card c = createCard(name, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Library).add(c, 0);
        return c;
    }

    @Test
    public void testDrawFromTheirLibraryIsYourDraw() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 3);
        fillLibrary(opp, 3);
        Card shivan = onTop("Shivan Dragon", opp);
        final int drawnBefore = me.getNumDrawnThisTurn();

        me.drawCards(1, null, AbilityKey.newMap(), me.getZone(ZoneType.Hand), opp);
        Card drawn = game.getCardState(shivan);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Hand).contains(drawn));
        AssertJUnit.assertEquals(opp, drawn.getOwner());
        AssertJUnit.assertEquals(me, drawn.getController());
        AssertJUnit.assertEquals(drawnBefore + 1, me.getNumDrawnThisTurn());
        AssertJUnit.assertEquals(3, me.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(3, opp.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testCombatDamageDrawsFromThatPlayer() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 3);
        fillLibrary(opp, 3);
        Card shivan = onTop("Shivan Dragon", opp);
        addCard("Spy Eye", me).setSickness(false);

        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals("the AI attacked with it", 19, opp.getLife());
        Card drawn = game.getCardState(shivan);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Hand).contains(drawn));
        AssertJUnit.assertEquals(opp, drawn.getOwner());
    }
}
