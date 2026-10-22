package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.ColorSet;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Your Favorite Missing Character (UNK), checked against Scryfall 2026-10-22 (no rulings): its deckbuilding choices are
 * made as the game begins (user ruling 2026-10-21).
 */
public class UnknownEventBatch35Test extends AITest {

    @Test
    public void testChoicesBecomePartOfTheCard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hero = addCardToZone("Your Favorite Missing Character", ai, ZoneType.Library);
        hero.writeMissingCharacter(ColorSet.fromNames("white", "blue"), java.util.List.of("Human", "Wizard"), "Professor Pip",
                "MissingTrigCast");
        AssertJUnit.assertEquals("Professor Pip", hero.getName());
        AssertJUnit.assertTrue(hero.getColor().hasWhite() && hero.getColor().hasBlue());
        AssertJUnit.assertTrue(hero.getType().hasCreatureType("Wizard"));
        AssertJUnit.assertTrue(hero.getTriggers().anyMatch(t -> t.getParam("TriggerDescription").contains("noncreature spell")));
        AssertJUnit.assertFalse(hero.getTriggers().anyMatch(t -> t.getParam("TriggerDescription").contains("surveil")));
        final Card moved = game.getAction().moveToPlay(hero, ai, null, null);
        AssertJUnit.assertEquals("kept in every zone", "Professor Pip", moved.getName());
    }

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass()
                .getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testAiChoosesAsTheGameBegins() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hero = addCardToZone("Your Favorite Missing Character", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertEquals(2, hero.getColor().countColors());
        AssertJUnit.assertFalse(hero.getName().equals("Your Favorite Missing Character"));
        AssertJUnit.assertTrue("an ability was added", hero.getTriggers().size() + hero.getStaticAbilities().size() >= 2);
    }
}
