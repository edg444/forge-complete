package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Genevieve, Conniving Dragon, Margle, Cousin of Yargle, The Judge of Height, Tarkiran Towelsbane and Drop Bear (UNK
 * rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch24Test extends AITest {

    @Test
    public void testMargleBlocksGainedAbilities() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card elves = addCard("Llanowar Elves", ai);
        addCard("Margle, Cousin of Yargle", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(elves.getManaAbilities().isEmpty());
        AssertJUnit.assertEquals(3, elves.getNetPower());
        addCard("Archetype of Imagination", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse("a later grant doesn't stick", elves.hasKeyword(Keyword.FLYING));
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility pump = forge.game.ability.AbilityFactory.getAbility("DB$ Pump | Defined$ Self | KW$ Trample", bears);
        pump.setActivatingPlayer(ai);
        AbilityUtils.resolve(pump);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.TRAMPLE));
    }

    @Test
    public void testMargleGoneGivesThemBack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card elves = addCard("Llanowar Elves", ai);
        final Card margle = addCard("Margle, Cousin of Yargle", ai);
        game.getAction().checkStaticAbilities();
        game.getAction().moveToGraveyard(margle, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(elves.getManaAbilities().isEmpty());
        AssertJUnit.assertEquals(1, elves.getNetPower());
    }

    @Test
    public void testJudgeOfHeight() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card judge = addCard("The Judge of Height", ai);
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", game.getPlayers().get(0));
        addCard("Colossal Dreadmaw", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("taller than the Bears (4) and the Elves (2), not the Dreadmaw (12)", 2, judge.getNetPower());
    }

    @Test
    public void testGenevieveRollsForTreasure() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card genevieve = addCard("Genevieve, Conniving Dragon", ai);
        addCard("Fiery Cannonade", ai);
        addCard("Thraben Inspector", ai);
        addCard("Suntail Hawk", ai);
        final SpellAbility combat = genevieve.getTriggers().get(0).ensureAbility();
        combat.setActivatingPlayer(ai);
        AbilityUtils.resolve(combat);
        AssertJUnit.assertEquals("no party, no die", 0, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().startsWith("Treasure")).count());
        addCard("Cleric of the Forward Order", ai);
        AbilityUtils.resolve(combat);
        final long treasures = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().startsWith("Treasure")).count();
        AssertJUnit.assertTrue("a d4 with a party of one: " + treasures, treasures >= 1 && treasures <= 4);
    }

    @Test
    public void testDropBear() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bear = addCard("Drop Bear", ai);
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCard("Grizzly Bears", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("two Forests, Grizzly Bears and itself", 4, bear.getNetPower());
        AssertJUnit.assertTrue(bear.getType().hasCreatureType("Koala"));
    }
}
