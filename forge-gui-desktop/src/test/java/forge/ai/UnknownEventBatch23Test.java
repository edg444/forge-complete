package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Majestic Duo, The Zabulous Cosplayer, Shen, Wish Granter, The Goblin Mastermind and The Brewing Chef (UNK rares),
 * checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch23Test extends AITest {

    @Test
    public void testBrewingChef() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card chef = addCard("The Brewing Chef", ai);
        final Card alarm = addCardToZone("Raise the Alarm", ai, ZoneType.Library);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        AssertJUnit.assertTrue(alarm.isValid("Card.CreatesToken", ai, chef, null));
        AssertJUnit.assertFalse(bears.isValid("Card.CreatesToken", ai, chef, null));
        final Card elf = addCard("Llanowar Elves", ai);
        elf.setGamePieceType(forge.card.GamePieceType.TOKEN);
        final Card ogre = addCard("Gray Ogre", ai);
        ogre.setGamePieceType(forge.card.GamePieceType.TOKEN);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("two differently named tokens", 3, elf.getNetPower());
    }

    @Test
    public void testGoblinMastermind() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card theirs = addCard("Grizzly Bears", game.getPlayers().get(0));
        addCard("The Goblin Mastermind", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(bears.getType().hasCreatureType("Goblin"));
        AssertJUnit.assertTrue(bolt.getType().hasCreatureType("Goblin"));
        AssertJUnit.assertTrue(bolt.getType().isKindred());
        AssertJUnit.assertFalse(theirs.getType().hasCreatureType("Goblin"));
    }

    @Test
    public void testShenScatters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card shen = addCard("Shen, Wish Granter", ai);
        final SpellAbility etb = shen.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(7, ai.getCardsIn(ZoneType.Library).stream().filter(c -> c.getName().equals("Dragonstorm Globe")).count());
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(shen, "Scattered", etb));
    }

    @Test
    public void testMajesticDuoCopy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card duo = addCard("The Majestic Duo", ai);
        final SpellAbility etb = duo.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card copy = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        AssertJUnit.assertFalse(copy.getType().isLegendary());
        AssertJUnit.assertEquals(1, copy.getTriggers().size());
        AssertJUnit.assertTrue(copy.getTriggers().get(0).getParam("TriggerDescription").contains("good luck"));
    }

    @Test
    public void testCosplayerBecomesSomething() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card cosplayer = addCard("The Zabulous Cosplayer", ai);
        final SpellAbility become = AbilityFactory.getAbility(cosplayer.getSVar("TrigCosplay"), cosplayer);
        become.setActivatingPlayer(ai);
        AbilityUtils.resolve(become);
        AssertJUnit.assertFalse(cosplayer.getName().equals("The Zabulous Cosplayer"));
        AssertJUnit.assertTrue(cosplayer.isPermanent());
        AssertJUnit.assertTrue("keeps the upkeep ability", cosplayer.getTriggers().stream()
                .anyMatch(t -> t.getParam("TriggerDescription") != null && t.getParam("TriggerDescription").contains("tournament-legal")));
    }
}
