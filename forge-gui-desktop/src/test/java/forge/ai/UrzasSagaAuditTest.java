package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Urza's Saga script review and R&D's Secret Lair printed text (2026-10-23). */
public class UrzasSagaAuditTest extends AITest {

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

    private boolean drakeStays(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card drake = printing("Gilded Drake", "USG", p, ZoneType.Hand);
        drake = game.getAction().moveToPlay(drake, p, null, null);
        resolveAll(game);
        return drake.isInPlay();
    }

    @Test
    public void testGildedDrakeWithNothingToTakeStaysAsPrinted() {
        AssertJUnit.assertTrue(drakeStays(true));
    }

    @Test
    public void testGildedDrakeWithNothingToTakeIsSacrificedByOracle() {
        AssertJUnit.assertFalse(drakeStays(false));
    }

    private int lifelineTriggers(final boolean lair, final boolean opponents) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        printing("Lifeline", "USG", p, ZoneType.Battlefield);
        addCard("Grizzly Bears", p);
        Card victim = addCard("Grizzly Bears", opponents ? opp : p);
        game.getAction().checkStateEffects(true);
        game.copyLastState();
        victim.setDamage(2);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        return game.getStack().size();
    }

    @Test
    public void testLifelineIgnoresOpponentsCreaturesAsPrinted() {
        AssertJUnit.assertEquals(0, lifelineTriggers(true, true));
        AssertJUnit.assertEquals(1, lifelineTriggers(true, false));
    }

    @Test
    public void testLifelineReturnsOpponentsCreaturesByOracle() {
        AssertJUnit.assertEquals(1, lifelineTriggers(false, true));
    }

    private boolean voidExilesItself(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card planar = printing("Planar Void", "USG", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        game.copyLastState();
        forge.game.spellability.SpellAbility kill = forge.game.ability.AbilityFactory.getAbility("DB$ Destroy | Defined$ Self", planar);
        kill.setActivatingPlayer(p);
        forge.game.ability.AbilityUtils.resolve(kill);
        resolveAll(game);
        return p.getCardsIn(ZoneType.Exile).stream().anyMatch(c -> c.getName().equals("Planar Void"));
    }

    @Test
    public void testPlanarVoidExilesItselfAsPrinted() {
        AssertJUnit.assertTrue(voidExilesItself(true));
    }

    @Test
    public void testPlanarVoidStaysInGraveyardByOracle() {
        AssertJUnit.assertFalse(voidExilesItself(false));
    }
}
