package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Sixth Stage of Magic Design (UNK white rare), checked against Scryfall 2026-10-22 (no rulings). What has a black
 * or silver expansion symbol is the user's ruling of 2026-10-22.
 */
public class UnknownEventBatch47Test extends AITest {

    private Card printing(final String name, final String set, final Player p) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private static int bonus(final Card c) {
        return c.getNetPower() - c.getBasePower();
    }

    @Test
    public void testSixthStage() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Sixth Stage of Magic Design", ai);
        final Card m10Common = printing("Griffin Sentinel", "M10", ai);
        final Card m10Rare = printing("Guardian Seraph", "M10", ai);
        final Card domCommon = printing("Aven Sentry", "DOM", ai);
        final Card domUncommon = printing("Knight of Grace", "DOM", ai);
        final Card cspCommon = printing("Kjeldoran Outrider", "CSP", ai);
        final Card iceRare = printing("Blinking Spirit", "ICE", ai);
        final Card alpha = printing("Grizzly Bears", "LEA", ai);
        final Card token = addToken("w_1_1_soldier", ai);
        game.getAction().checkStaticAbilities();

        AssertJUnit.assertEquals("common: black", 1, bonus(m10Common));
        AssertJUnit.assertEquals("rare: gold", 2, bonus(m10Rare));
        AssertJUnit.assertEquals("Dominaria's commons are white", 2, bonus(domCommon));
        AssertJUnit.assertEquals("uncommons are silver everywhere", 1, bonus(domUncommon));
        AssertJUnit.assertEquals("Coldsnap's commons are white", 2, bonus(cspCommon));
        AssertJUnit.assertEquals("before Exodus every symbol is black", 1, bonus(iceRare));
        AssertJUnit.assertEquals("Alpha has no symbol", 2, bonus(alpha));
        AssertJUnit.assertEquals("tokens are black", 1, bonus(token));
        AssertJUnit.assertTrue(alpha.getType().isLegendary() && token.getType().isLegendary());
    }
}
