package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityJumpStartNotExiled;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * The Asker of More Mana, The Keeper of Favorite Cards, The Water Maro, The Vegetable Car, Jiffy, Vehicle Repairer,
 * Uh, Squen &amp; Cragg Made a Card, The Herald of Numot and The Weekly Princess (UNK), checked against Scryfall
 * 2026-10-21 (no rulings).
 */
public class UnknownEventBatch20Test extends AITest {

    @Test
    public void testAskerTaxesEveryPayment() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card memnite = addCardToZone("Memnite", ai, ZoneType.Hand);
        addCard("The Asker of More Mana", game.getPlayers().get(0));
        final SpellAbility cast = bears.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        ManaCostBeingPaid cost = new ManaCostBeingPaid(bears.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertEquals(3, cost.getConvertedManaCost());
        cost = new ManaCostBeingPaid(bears.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, true);
        AssertJUnit.assertEquals("an effect's mana payment too", 3, cost.getConvertedManaCost());
        final SpellAbility free = memnite.getFirstSpellAbility();
        free.setActivatingPlayer(ai);
        cost = new ManaCostBeingPaid(memnite.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, free, ai, null, true, false);
        AssertJUnit.assertTrue("paying nothing isn't paying mana", cost.isPaid());
    }

    @Test
    public void testFavorites() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card keeper = addCard("The Keeper of Favorite Cards", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card theirBears = addCard("Grizzly Bears", game.getPlayers().get(0));
        ai.addFavoriteCardName("Grizzly Bears");
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals("only the favorites you control", 2, theirBears.getNetPower());
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(bolt.isValid("Card.nonCreature+YourFavorite", ai, keeper, null));
        ai.addFavoriteCardName("Lightning Bolt");
        AssertJUnit.assertTrue(bolt.isValid("Card.nonCreature+YourFavorite", ai, keeper, null));
    }

    @Test
    public void testWaterMaroCounts() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card maro = addCard("The Water Maro", ai);
        addCard("Maro", ai);
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", ai).setGamePieceType(forge.card.GamePieceType.TOKEN);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("itself, Maro and the token", 3, maro.getNetPower());
    }

    @Test
    public void testOnionfect() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card car = addCard("The Vegetable Car", ai);
        final Card bears = addCard("Grizzly Bears", human);
        bears.addDamageAfterPrevention(2, car, null, false, new GameEntityCounterTable());
        AssertJUnit.assertEquals(0, bears.getDamage());
        human.addDamageAfterPrevention(3, car, null, false, new GameEntityCounterTable());
        AssertJUnit.assertEquals(5, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Food")).count());
    }

    @Test
    public void testSquenAndCragg() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        addCard("Uh, Squen & Cragg Made a Card", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(1, bears.getNetPower());
        AssertJUnit.assertEquals(1, bears.getNetToughness());
    }

    @Test
    public void testJiffy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card cart = addCardToZone("Smuggler's Copter", ai, ZoneType.Graveyard);
        AssertJUnit.assertFalse(StaticAbilityJumpStartNotExiled.notExiled(ai));
        addCard("Jiffy, Vehicle Repairer", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(cart.hasKeyword("Jump-start"));
        AssertJUnit.assertTrue(StaticAbilityJumpStartNotExiled.notExiled(ai));
        AssertJUnit.assertFalse(StaticAbilityJumpStartNotExiled.notExiled(game.getPlayers().get(0)));
    }

    @Test
    public void testHeraldMakesNumot() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card herald = addCard("The Herald of Numot", ai);
        final SpellAbility trig = herald.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Library).anyMatch(c -> c.getName().equals("Numot, the Devastator")));
    }

    @Test
    public void testWeeklyPrincess() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card princess = addCard("The Weekly Princess", ai);
        final Trigger dawn = princess.getTriggers().stream().filter(t -> "Day".equals(t.getParam("To"))).findFirst().orElseThrow();
        game.setDayTime(true);
        AssertJUnit.assertFalse(dawn.performTest(null));
        game.setDayTime(false);
        AssertJUnit.assertTrue(dawn.performTest(null));
        final SpellAbility leave = princess.getTriggers().stream().filter(t -> t.getParam("TriggerDescription").contains("stops being"))
                .findFirst().orElseThrow().ensureAbility();
        leave.setActivatingPlayer(ai);
        AbilityUtils.resolve(leave);
        AssertJUnit.assertTrue(game.isNeitherDayNorNight());
    }
}
