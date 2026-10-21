package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Park Map (UNF #190), checked against Scryfall and its rulings 2026-10-21: the same art is the same illustration,
 * whatever the printing.
 */
public class ParkMapTest extends AITest {

    private Card printing(final String set, final String cn, final Player p, final ZoneType zone) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Forest", set, cn);
        final Card c = Card.fromPaperCard(pc, p);
        p.getZone(zone).add(c);
        return c;
    }

    @Test
    public void testSameArt() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card map = addCard("Park Map", p);
        printing("UNF", "239", p, ZoneType.Battlefield);
        final Card same = printing("UNF", "239", p, ZoneType.Library);
        final Card other = printing("M19", "277", p, ZoneType.Library);
        AssertJUnit.assertNotNull(same.getKnownArt());
        AssertJUnit.assertTrue(same.isValid("Land.sameArtAsPermanentYouCtrl", p, map, null));
        AssertJUnit.assertFalse(other.isValid("Land.sameArtAsPermanentYouCtrl", p, map, null));
    }
}
