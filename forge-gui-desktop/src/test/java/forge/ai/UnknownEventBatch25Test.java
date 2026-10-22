package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Wheel of Not Ideal, The Cobra King, Autumn Willow and Baron Sengir, The Bird Champion and Emerin, Shivan Angel (UNK),
 * checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch25Test extends AITest {

    @Test
    public void testWheelPassesLibraries() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 8; i++) {
            addCardToZone("Island", human, ZoneType.Library);
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card wheel = addCardToZone("Wheel of Not Ideal", ai, ZoneType.Hand);
        final SpellAbility cast = wheel.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue("the AI drew from the Islands passed to it",
                ai.getCardsIn(ZoneType.Hand).allMatch(c -> c.getName().equals("Island") && c.getOwner() == human));
        AssertJUnit.assertEquals(7, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Hand).allMatch(c -> c.getName().equals("Mountain")));
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Graveyard).anyMatch(c -> c.getName().equals("Lightning Bolt")));
    }

    @Test
    public void testCobraKingMakesCobraCoils() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card king = addCard("The Cobra King", ai);
        final SpellAbility upkeep = king.getTriggers().get(0).ensureAbility();
        upkeep.setActivatingPlayer(ai);
        AbilityUtils.resolve(upkeep);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getName().equals("Cobra Coil") && c.getType().hasCreatureType("Serpent")));
    }

    @Test
    public void testWillowAndSengir() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card pair = addCard("Autumn Willow and Baron Sengir", ai);
        final Card vampire = addCard("Vampire Nighthawk", ai);
        final Card other = addCard("Sengir Vampire", ai);
        final Card victim = addCard("Grizzly Bears", human);
        final SpellAbility bite = forge.game.ability.AbilityFactory.getAbility("DB$ DealDamage | NumDmg$ 1 | Defined$ CardUID_" + victim.getId(), vampire);
        bite.setActivatingPlayer(ai);
        AbilityUtils.resolve(bite);
        final Card lki = forge.game.card.CardCopyService.getLKICopy(victim);
        final SpellAbility dies = pair.getTriggers().get(0).ensureAbility();
        dies.setActivatingPlayer(ai);
        dies.setTriggeringObject(AbilityKey.CardLKI, lki);
        dies.setTriggeringObject(AbilityKey.Card, lki);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertEquals(1, vampire.getCounters(CounterEnumType.P2P2));
        AssertJUnit.assertEquals("dealt it no damage", 0, other.getCounters(CounterEnumType.P2P2));
    }

    @Test
    public void testBirdChampion() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card champion = addCard("The Bird Champion", ai);
        addCard("Suntail Hawk", ai);
        final SpellAbility upkeep = champion.getTriggers().get(0).ensureAbility();
        upkeep.setActivatingPlayer(ai);
        AbilityUtils.resolve(upkeep);
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasCreatureType("Bird")).count());
        for (int i = 0; i < 8; i++) {
            addCard("Suntail Hawk", ai);
        }
        ai.setLife(0, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("twelve Birds", ai.hasLost());
    }

    @Test
    public void testEmerinGrantsAngelsAndDragons() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Emerin, Shivan Angel", ai);
        final Card dragon = addCard("Shivan Dragon", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(dragon.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue(dragon.hasKeyword(Keyword.WARD));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.LIFELINK));
    }
}
