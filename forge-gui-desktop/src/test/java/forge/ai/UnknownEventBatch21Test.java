package forge.ai;

import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * The Fifth Alias, Gomif, Fast Racer, The Mox Painter, Frie, Displaced in Time, Pirk, Heroic Captain and The Wizardly
 * Barge (UNK rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch21Test extends AITest {

    @Test
    public void testGomifUncapsSpeed() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ai.setSpeed(4);
        ai.increaseSpeed();
        AssertJUnit.assertEquals(4, ai.getSpeed());
        final Card gomif = addCard("Gomif, Fast Racer", ai);
        game.getAction().checkStaticAbilities();
        ai.increaseSpeed();
        AssertJUnit.assertEquals(5, ai.getSpeed());
        AssertJUnit.assertTrue("still max speed", ai.maxSpeed());
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(5, gomif.getNetPower());
    }

    @Test
    public void testMoxPainterCompletesTheSet() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card painter = addCard("The Mox Painter", ai);
        for (final String mox : new String[] {"Mox Amber", "Chrome Mox", "Mox Diamond", "Mox Emerald", "Mox Jet", "Mox Opal",
                "Mox Pearl", "Mox Ruby", "Mox Sapphire"}) {
            addCard(mox, ai);
        }
        final SpellAbility paint = painter.getSpellAbilities().stream().filter(s -> s.getApi() == forge.game.ability.ApiType.Cleanup).findFirst().orElseThrow();
        paint.setActivatingPlayer(ai);
        AbilityUtils.resolve(paint);
        AssertJUnit.assertTrue("the one missing", ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getName().equals("Mox Tantalite")));
        AssertJUnit.assertTrue("all ten", ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getName().equals("Mox Lotus")));
        final int before = ai.getCardsIn(ZoneType.Battlefield).size();
        AbilityUtils.resolve(paint);
        AssertJUnit.assertEquals("no Mox left to paint; already has a Lotus but makes another", before + 1,
                ai.getCardsIn(ZoneType.Battlefield).size());
    }

    @Test
    public void testWizardlyBarge() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card barge = addCard("The Wizardly Barge", ai);
        final SpellAbility attack = barge.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        AbilityUtils.resolve(attack);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.isToken() && c.isCreature() && c.getType().hasCreatureType("Wizard")));
    }

    @Test
    public void testSecretList() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card alias = addCard("The Fifth Alias", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(bolt.isValid("Card.OnYourSecretList", ai, alias, null));
        ai.addToSecretList(bolt);
        AssertJUnit.assertTrue(bolt.isValid("Card.OnYourSecretList", ai, alias, null));
    }

    @Test
    public void testWarpExileCause() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility warped = AbilityFactory.getAbility("DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile | WarpExile$ True", bears);
        final SpellAbility plain = AbilityFactory.getAbility("DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile", bears);
        AssertJUnit.assertTrue(warped.isValid("Ability.WarpExile", ai, bears, null));
        AssertJUnit.assertFalse(plain.isValid("Ability.WarpExile", ai, bears, null));
    }

    @Test
    public void testPirk() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card pirk = addCard("Pirk, Heroic Captain", ai);
        final SpellAbility etb = pirk.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card ship = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Spacecraft")).findFirst().orElseThrow();
        AssertJUnit.assertFalse("named", ship.getName().equals("Spacecraft Token"));
        final Trigger station = pirk.getTriggers().get(1);
        ship.addCounterInternal(CounterEnumType.CHARGE, 4, ai, false, null, null);
        final Map<AbilityKey, Object> run = AbilityKey.mapFromCard(ship);
        run.put(AbilityKey.CounterType, forge.game.card.CounterType.getType("CHARGE"));
        run.put(AbilityKey.CounterAmount, 4);
        AssertJUnit.assertFalse(station.performTest(run));
        ship.addCounterInternal(CounterEnumType.CHARGE, 3, ai, false, null, null);
        run.put(AbilityKey.CounterAmount, 3);
        AssertJUnit.assertTrue("4 to 7 crosses 6", station.performTest(run));
        ship.addCounterInternal(CounterEnumType.CHARGE, 2, ai, false, null, null);
        run.put(AbilityKey.CounterAmount, 2);
        AssertJUnit.assertFalse("already fully stationed", station.performTest(run));
    }
}
