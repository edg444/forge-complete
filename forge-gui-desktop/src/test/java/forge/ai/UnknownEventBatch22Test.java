package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Ice Dancer, The Sad Creaturez, Tarelily, Lovestruck Archer, Princess Snowfall, The Bus Runner and The River
 * Warlock (UNK rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch22Test extends AITest {

    @Test
    public void testSadCreaturez() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("The Sad Creaturez", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(CombatUtil.canAttack(bears));
        final SpellAbility share = bears.getSpellAbilities().stream().filter(s -> s.getDescription().contains("feelings")).findFirst().orElseThrow();
        share.setActivatingPlayer(ai);
        AbilityUtils.resolve(share);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("shared its feelings", CombatUtil.canAttack(bears));
        AssertJUnit.assertEquals(1, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(21, ai.getLife());
    }

    @Test
    public void testIceDancer() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card dancer = addCard("The Ice Dancer", ai);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        final SpellAbility hit = dancer.getTriggers().get(0).ensureAbility();
        hit.setActivatingPlayer(ai);
        AbilityUtils.resolve(hit);
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(21, ai.getLife());
        AssertJUnit.assertEquals(19, human.getLife());
    }

    @Test
    public void testTarelilyPairedWithDwarf() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tarelily = addCard("Tarelily, Lovestruck Archer", ai);
        final Card dwarf = addCard("Dwarven Trader", ai);
        tarelily.setPairedWith(dwarf);
        dwarf.setPairedWith(tarelily);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(dwarf.hasKeyword(Keyword.REACH));
        AssertJUnit.assertTrue(tarelily.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue(dwarf.hasKeyword(Keyword.LIFELINK));
    }

    @Test
    public void testSnowfallMakesDwarves() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card snowfall = addCard("Princess Snowfall", ai);
        final SpellAbility etb = snowfall.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.isToken() && c.getName().equals("Seven Dwarves")));
    }

    @Test
    public void testBusRunnerHours() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card runner = addCard("The Bus Runner", ai);
        final SpellAbility etb = runner.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card bus = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Desert")).findFirst().orElseThrow();
        AssertJUnit.assertEquals(8, bus.getCounters(CounterEnumType.HOUR));
        bus.subtractCounter(CounterEnumType.HOUR, 7, null);
        final SpellAbility tick = bus.getTriggers().get(0).ensureAbility();
        tick.setActivatingPlayer(ai);
        AbilityUtils.resolve(tick);
        AssertJUnit.assertEquals("back to eight", 8, bus.getCounters(CounterEnumType.HOUR));
        AssertJUnit.assertEquals(19, human.getLife());
        AssertJUnit.assertEquals(21, ai.getLife());
    }

    @Test
    public void testRiverWarlock() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card warlock = addCard("The River Warlock", ai);
        addCardToZone("Gray Ogre", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", human, ZoneType.Library);
        addCardToZone("Lightning Bolt", human, ZoneType.Hand);
        final SpellAbility attack = warlock.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        attack.setTriggeringObject(AbilityKey.DefendingPlayer, human);
        AbilityUtils.resolve(attack);
        AssertJUnit.assertEquals("discarded the 3 against a 1 or a 2: the other one is exiled", 0, human.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Exile).size());
    }
}
