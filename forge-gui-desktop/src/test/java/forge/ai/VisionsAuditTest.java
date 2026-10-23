package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Visions script review and R&D's Secret Lair printed text (2026-10-23). */
public class VisionsAuditTest extends AITest {

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

    private int rowenDrawOutsideDrawStep(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        printing("Rowen", "VIS", p, ZoneType.Battlefield);
        printing("Island", "VIS", p, ZoneType.Library);
        printing("Forest", "VIS", p, ZoneType.Library);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        int before = p.getCardsIn(ZoneType.Hand).size();
        p.drawCard();
        resolveAll(game);
        return p.getCardsIn(ZoneType.Hand).size() - before;
    }

    @Test
    public void testRowenOnlyInYourDrawStepAsPrinted() {
        AssertJUnit.assertEquals("Oracle: the first card drawn each turn", 2, rowenDrawOutsideDrawStep(false));
        AssertJUnit.assertEquals("as printed: only during your draw phase", 1, rowenDrawOutsideDrawStep(true));
    }

    @Test
    public void testBogardanPhoenixReturnsWithADeathCounter() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card phoenix = addCard("Bogardan Phoenix", p);
        game.getAction().checkStateEffects(true);
        game.copyLastState();
        phoenix.setDamage(3);
        resolveAll(game);
        Card back = p.getCreaturesInPlay().getFirst();
        AssertJUnit.assertEquals("Bogardan Phoenix", back.getName());
        AssertJUnit.assertEquals(1, back.getCounters(forge.game.card.CounterEnumType.DEATH));
    }
}
