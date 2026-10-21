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
 * Old Way Phyrexian, Hidetsugu's Poison Rite, Invasion Specialist, Quest Compleated Beast, Rankle, Master of
 * Pranksters, The Swamp Kitten, Rat of God, Davvol's Birthdaymobile and Auntie Flint (UNK RB03a-RB13a), checked
 * against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch12Test extends AITest {

    @Test
    public void testOldWayPhyrexian() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card toxic = addCard("Bilious Skulldweller", ai);
        AssertJUnit.assertTrue(toxic.hasKeyword(Keyword.TOXIC));
        addCard("Old Way Phyrexian", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse("loses toxic", toxic.hasKeyword(Keyword.TOXIC));
        AssertJUnit.assertTrue("gains infect", toxic.hasKeyword(Keyword.INFECT));
    }

    @Test
    public void testPoisonRiteTargets() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card rite = addCardToZone("Hidetsugu's Poison Rite", ai, ZoneType.Hand);
        human.setCounters(CounterEnumType.POISON, 6, null, false);
        AssertJUnit.assertFalse(human.isValid("Player.poisonCountersEQ7", ai, rite, rite.getFirstSpellAbility()));
        human.setCounters(CounterEnumType.POISON, 7, null, false);
        AssertJUnit.assertTrue(human.isValid("Player.poisonCountersEQ7", ai, rite, rite.getFirstSpellAbility()));
    }

    @Test
    public void testQuestCompleatedBeastPoison() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Quest Compleated Beast", ai);
        human.setCounters(CounterEnumType.POISON, 3, null, false);
        human.subtractCounter(CounterEnumType.POISON, 2, human);
        AssertJUnit.assertEquals("can't remove poison counters", 3, human.getPoisonCounters());
        human.gainLife(5, null, null);
        AssertJUnit.assertEquals("can't gain life", 20, human.getLife());
    }

    @Test
    public void testRatOfGod() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", human);
        addCard("Typhoid Rats", human);
        addCard("Typhoid Rats", ai);
        addCard("Typhoid Rats", ai);
        final Card rat = addCardToZone("Rat of God", ai, ZoneType.Hand);
        final SpellAbility sa = rat.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Battlefield).stream().noneMatch(c -> c.getName().equals("Grizzly Bears")));
        final Card army = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Army")).findFirst().orElseThrow();
        AssertJUnit.assertEquals("amassed two for its two Rats", 2, army.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testAuntieFlintMonoBlack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Typhoid Rats", ai);
        addCard("Auntie Flint", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("mono-black: +1/+0", 2, bears.getNetPower());
        addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(1, bears.getNetPower());
    }

    @Test
    public void testSwampKitten() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card swamp = addCard("Swamp", ai);
        final Card kitten = addCard("The Swamp Kitten", ai);
        final SpellAbility trig = kitten.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.getTargets().add(swamp);
        AbilityUtils.resolve(trig);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(swamp.isCreature());
        AssertJUnit.assertTrue(swamp.isLand());
        AssertJUnit.assertEquals("0/0, a counter, and the Kitten's +1/+1", 2, swamp.getNetPower());
        AssertJUnit.assertTrue(swamp.hasKeyword(Keyword.MENACE));
    }

    @Test
    public void testRankleAndBirthdaymobile() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card rankle = addCard("Rankle, Master of Pranksters", ai);
        final SpellAbility charm = rankle.getTriggers().get(0).ensureAbility();
        int expected = 0;
        for (final SpellAbility mode : charm.getAdditionalAbilityList("Choices")) {
            mode.setActivatingPlayer(ai);
            AbilityUtils.resolve(mode);
            expected++;
            AssertJUnit.assertEquals("each player made a copy of " + mode.getParam("DefinedName"), expected,
                    human.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).count());
        }
        final Card mobile = addCard("Davvol's Birthdaymobile", ai);
        final SpellAbility etb = mobile.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(human);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.getName().equals("Carnival of Souls")));
    }
}
