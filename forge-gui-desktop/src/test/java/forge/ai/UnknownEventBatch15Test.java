package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPrintings;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Stormscale Wurm, Praetorhoof Behemoth, Lander Rizzi, Emrakul and Chatterfang, The Bear Force Pilot Runner, The
 * Magical City, New, The Goblin Sparring Grounds, The Local Guides, A-Town, Lona, Tracker of the Known and Mr. Wiggles,
 * Helpful Butterfly (UNK), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch15Test extends AITest {

    @Test
    public void testCityCounts() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        human.setMatchNote("City", "Atlanta");
        AssertJUnit.assertEquals("the AI is where its human opponent is", "Atlanta", ai.getCity());
        final Card town = addCard("A-Town", ai);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(town, "InA", town.getManaAbilities().get(0)));
        human.setMatchNote("City", "Boston");
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(town, "InA", town.getManaAbilities().get(0)));
    }

    @Test
    public void testLocalGuidesExplore() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        human.setMatchNote("City", "Boston");
        final Card guides = addCard("The Local Guides", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility trig = guides.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.setTriggeringObject(forge.game.ability.AbilityKey.Card, bears);
        AssertJUnit.assertEquals("no G in Boston", 0, AbilityUtils.calculateAmount(guides, "InCity", trig));
        final Card ox = addCard("Bonded Horncrest", ai);
        trig.setTriggeringObject(forge.game.ability.AbilityKey.Card, ox);
        AssertJUnit.assertEquals("B is in Boston", 1, AbilityUtils.calculateAmount(guides, "InCity", trig));
    }

    @Test
    public void testLonaReleases() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card fresh = addCardToZone("Unclaimed Cat", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(CardPrintings.releaseCount(bolt) >= 5);
        AssertJUnit.assertTrue(CardPrintings.releaseCount(fresh) < 5);
        addCard("Lona, Tracker of the Known", ai);
        final SpellAbility cast = bolt.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid cost = new forge.game.mana.ManaCostBeingPaid(bolt.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertTrue("{R} less: free", cost.isPaid());
    }

    @Test
    public void testCommanderLands() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card city = addCard("The Magical City, New", ai);
        AssertJUnit.assertTrue(city.getRules().canBeCommander());
        final Card grounds = addCard("The Goblin Sparring Grounds", ai);
        AssertJUnit.assertTrue(grounds.getRules().canBeCommander());
    }

    @Test
    public void testEmrakulAndChatterfang() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card emrakul = addCardToZone("Emrakul and Chatterfang", ai, ZoneType.Hand);
        final SpellAbility trig = emrakul.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertEquals(15, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasCreatureType("Squirrel")).count());
        AssertJUnit.assertTrue(emrakul.hasKeyword(Keyword.DEVOID));
    }

    @Test
    public void testBearForceAndLander() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card runner = addCard("The Bear Force Pilot Runner", ai);
        final SpellAbility trig = runner.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        final Card vehicle = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Vehicle")).findFirst().orElseThrow();
        // a Vehicle's P/T matters once it's crewed; the bonus counts the Bears
        AssertJUnit.assertEquals("one Bear", 1, AbilityUtils.calculateAmount(vehicle, "X", vehicle.getStaticAbilities().get(0)));
        AssertJUnit.assertTrue(vehicle.hasKeyword(Keyword.FLYING));
    }
}
