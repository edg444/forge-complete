package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Devoted Abzan, Bad Knight, Impressive Rat, Vamping Vampire, Soul Drainer, Would You Have Done the Same?, Sheoldred's
 * Terror and Windmill Slam (UNK uncommons), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch29Test extends AITest {

    @Test
    public void testDevotionToAbzan() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card abzan = addCard("Devoted Abzan", ai);
        addCard("Siege Rhino", ai);
        addCard("Lightning Bolt", ai);
        final SpellAbility draw = abzan.getSpellAbilities().getFirst();
        draw.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Abzan's {B} and the Rhino's {W}{B}{G}", 4, AbilityUtils.calculateAmount(abzan, "X", draw));
    }

    @Test
    public void testBadKnightScoresAtGameEnd() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Bad Knight", ai);
        final int before = forge.game.ability.effects.ChooseTeamEffect.teamPoints(ai, forge.game.ability.effects.ChooseTeamEffect.PHYREXIAN);
        game.setGameOver(forge.game.GameEndReason.Draw);
        AssertJUnit.assertEquals(before + 1, forge.game.ability.effects.ChooseTeamEffect.teamPoints(ai, forge.game.ability.effects.ChooseTeamEffect.PHYREXIAN));
    }

    @Test
    public void testImpressiveRatIntrigues() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card rat = addCard("Impressive Rat", ai);
        final Card land = addCard("Forest", human);
        final SpellAbility dies = rat.getTriggers().get(0).ensureAbility();
        dies.setActivatingPlayer(ai);
        dies.getTargets().add(land);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertEquals(1, land.getCounters(CounterType.getType("HOLE")));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Command).anyMatch(c -> c.getTriggers().anyMatch(t -> t.getParam("TriggerDescription").contains("investigate"))));
    }

    @Test
    public void testVampingVampireStalls() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card vampire = addCardToZone("Vamping Vampire", ai, ZoneType.Exile);
        final SpellAbility stall = vampire.getTriggers().get(0).ensureAbility();
        stall.setActivatingPlayer(ai);
        AbilityUtils.resolve(stall);
        AbilityUtils.resolve(stall);
        AssertJUnit.assertEquals(2, vampire.getCounters(CounterType.getType("STALL")));
    }

    @Test
    public void testSoulDrainerForgives() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCard("Grizzly Bears", human);
        human.addCommander(commander);
        ai.addCommanderDamage(commander, 15);
        final Card drainer = addCard("Soul Drainer", ai);
        final SpellAbility etb = drainer.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(commander);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(0, ai.getCommanderDamage(commander));
    }

    @Test
    public void testSheoldredsTerror() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card terror = addCardToZone("Sheoldred's Terror", ai, ZoneType.Hand);
        final Card memnite = addCard("Memnite", human);
        final Card bears = addCard("Grizzly Bears", human);
        final SpellAbility cast = terror.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(cast.canTarget(memnite));
        AssertJUnit.assertTrue(cast.canTarget(bears));
    }

    @Test
    public void testWindmillSlam() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card slam = addCardToZone("Windmill Slam", ai, ZoneType.Hand);
        final SpellAbility cast = slam.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AbilityUtils.resolve(cast);
        final Card mill = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().startsWith("Windmill")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(mill.getType().hasCreatureType("Windmill"));
        AssertJUnit.assertEquals(4, mill.getNetPower());
    }
}
