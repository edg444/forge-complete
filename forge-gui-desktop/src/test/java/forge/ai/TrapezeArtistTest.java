package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Trapeze Artist (UNF #32), checked against Scryfall and its 3 rulings 2026-10-21: it enters by being flipped (a
 * simulated half-and-half, as for Orcish Paratroopers), and a botched flip sends it straight back to its owner's hand.
 */
public class TrapezeArtistTest extends AITest {

    @Test
    public void testItEitherStaysOrGoesBackToHand() {
        int stayed = 0;
        int returned = 0;
        for (int i = 0; i < 40; i++) {
            final Game game = initAndCreateGame();
            final Player p = game.getPlayers().get(1);
            final Card artist = addCardToZone("Trapeze Artist", p, ZoneType.Hand);
            game.getAction().moveToPlay(artist, null, null);
            game.getAction().checkStateEffects(true);
            final Card now = game.getCardState(artist);
            if (now.isInZone(ZoneType.Battlefield)) {
                stayed++;
            } else {
                AssertJUnit.assertTrue("back to its owner's hand", now.isInZone(ZoneType.Hand));
                returned++;
            }
            AssertJUnit.assertTrue("no die is rolled", p.getNumRollsThisTurn() == 0);
        }
        AssertJUnit.assertTrue("both happen: " + stayed + "/" + returned, stayed > 0 && returned > 0);
    }
}
