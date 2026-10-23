package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Exodus script review and R&D's Secret Lair printed text (2026-10-23). */
public class ExodusAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static boolean citySurvivesItsOwnPlay(final Game game, final Player p) {
        Card city = printing("City of Traitors", "EXO", p, ZoneType.Hand);
        city = p.playLand(city, null);
        resolveAll(game);
        return city.isInPlay();
    }

    @Test
    public void testCityOfTraitorsSeesItselfPlayedAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        AssertJUnit.assertFalse(citySurvivesItsOwnPlay(game, p));
    }

    @Test
    public void testCityOfTraitorsIgnoresItselfByOracle() {
        Game game = initAndCreateGame();
        AssertJUnit.assertTrue(citySurvivesItsOwnPlay(game, game.getPlayers().get(1)));
    }
}
