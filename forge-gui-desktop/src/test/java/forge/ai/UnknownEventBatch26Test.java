package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Ano'thr, Equipment Commander, Zimone's Homework, First Stage of Magic Design and The Dogronmaster (UNK rares),
 * checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch26Test extends AITest {

    @Test
    public void testLifeSequences() {
        AssertJUnit.assertTrue(AbilityUtils.lifeInSequence(22, "LazyCaterer"));
        AssertJUnit.assertFalse(AbilityUtils.lifeInSequence(20, "LazyCaterer"));
        AssertJUnit.assertTrue(AbilityUtils.lifeInSequence(21, "Fibonacci"));
        AssertJUnit.assertFalse(AbilityUtils.lifeInSequence(20, "Fibonacci"));
        AssertJUnit.assertTrue(AbilityUtils.lifeInSequence(16, "Square"));
        AssertJUnit.assertFalse(AbilityUtils.lifeInSequence(20, "Square"));
    }

    @Test
    public void testZimonesHomework() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card homework = addCard("Zimone's Homework", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card ogre = addCard("Gray Ogre", ai);
        ai.setLife(16, null);
        final SpellAbility end = homework.getTriggers().get(0).ensureAbility();
        end.setActivatingPlayer(ai);
        AbilityUtils.resolve(end);
        AssertJUnit.assertTrue("16 is on the lazy caterer's sequence", ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getName().startsWith("Food")));
        AssertJUnit.assertFalse("but not a Fibonacci number", ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getType().hasCreatureType("Fractal")));
        AssertJUnit.assertEquals("and is a square", 3, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("2/2 but not 2/1... Gray Ogre is 2/2 too", 3, ogre.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testAnothrModes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card anothr = addCard("Ano'thr, Equipment Commander", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility pickAnthem = forge.game.ability.AbilityFactory.getAbility(anothr.getSVar("PickAnthem"), anothr);
        pickAnthem.setActivatingPlayer(ai);
        AbilityUtils.resolve(pickAnthem);
        final Card sword = addCard("Bonesplitter", ai);
        sword.attachToEntity(bears, null, true);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("Bonesplitter's +2 and the anthem's +1", 5, bears.getNetPower());
        AssertJUnit.assertFalse("haste wasn't chosen", bears.hasKeyword(Keyword.HASTE));
    }

    @Test
    public void testDogronmaster() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card dog = addCard("The Dogronmaster", ai);
        final SpellAbility etb = dog.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.setTriggeringObject(forge.game.ability.AbilityKey.NewCard, dog);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(dog.getAttachedCards().anyMatch(c -> c.getName().equals("Armadillo Cloak")));
    }
}
