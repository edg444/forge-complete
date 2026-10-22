package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Lord of Atlanta, The Karfell Rocker, Rinta, Cousin of Sitka, The Clever Magician and The Misty Stepper (UNK blue
 * rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch17Test extends AITest {

    @Test
    public void testLordOfAtlanta() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card merfolk = addCard("Merfolk of the Pearl Trident", ai);
        final Card cat = addCard("Unclaimed Cat", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        addCard("Lord of Atlanta", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(2, merfolk.getNetPower());
        AssertJUnit.assertEquals("a playtest creature", 4, cat.getNetPower());
        AssertJUnit.assertEquals(2, bears.getNetPower());
        AssertJUnit.assertTrue(merfolk.hasKeyword(Keyword.LANDWALK));
    }

    @Test
    public void testKarfellRockerLoud() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rocker = addCard("The Karfell Rocker", ai);
        final Card big = addCardToZone("Colossal Dreadmaw", ai, ZoneType.Hand);
        final Card wordy = addCardToZone("Ajani's Pridemate", ai, ZoneType.Hand);
        final Card wordy3 = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        final Card three = addCardToZone("Fountain of Youth", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(big.isValid("Card.cmcGE5,Card.wordsInNameGE3".split(","), ai, rocker, null));
        AssertJUnit.assertTrue("three words", three.isValid("Card.cmcGE5,Card.wordsInNameGE3".split(","), ai, rocker, null));
        AssertJUnit.assertFalse(wordy.isValid("Card.cmcGE5,Card.wordsInNameGE3".split(","), ai, rocker, null));
        AssertJUnit.assertFalse(wordy3.isValid("Card.cmcGE5,Card.wordsInNameGE3".split(","), ai, rocker, null));
    }

    @Test
    public void testRintaReducesPlaytest() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Rinta, Cousin of Sitka", ai);
        final Card cat = addCardToZone("Unclaimed Cat", ai, ZoneType.Hand);
        final SpellAbility cast = cat.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid cost = new forge.game.mana.ManaCostBeingPaid(cat.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertEquals(2, cost.getConvertedManaCost());
    }

    @Test
    public void testMistyStepperSpellsHaveSplitSecond() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stepper = addCard("The Misty Stepper", ai);
        AssertJUnit.assertEquals(2, stepper.getStaticAbilities().size());
        final Card magician = addCard("The Clever Magician", ai);
        AssertJUnit.assertEquals(2, magician.getTriggers().size());
    }
}
