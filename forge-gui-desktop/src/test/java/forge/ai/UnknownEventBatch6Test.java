package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Underdog Racer, Unclaimed Tanadon, Take the High Ground, Tarkir Omenpath, Unknown Event Shores, Command Mine, Command
 * Power Plant, Fear of Going 0-2 Drop, Aggressive Detective and Join the Winning Team (UNK CG05a-CR03d), checked
 * against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch6Test extends AITest {

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass()
                .getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testScoreboardAndUnderdog() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        human.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        final Card racer = addCardToZone("Underdog Racer", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("tied: not behind", 0, AbilityUtils.calculateAmount(racer, "Behind", null));

        final Card bears = addCard("Grizzly Bears", human);
        final Card giant = addCard("Hill Giant", human);
        final Card ground = addCardToZone("Take the High Ground", human, ZoneType.Hand);
        final SpellAbility sa = ground.getFirstSpellAbility();
        sa.setActivatingPlayer(human);
        sa.getTargets().add(giant);
        final Card aiBears = addCard("Grizzly Bears", ai);
        sa.getSubAbility().getTargets().add(aiBears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(1, ChooseTeamEffect.teamPoints(ai, ChooseTeamEffect.MIRRAN));
        AssertJUnit.assertEquals("Phyrexians behind", 1, AbilityUtils.calculateAmount(racer, "Behind", null));
        AssertJUnit.assertTrue(bears.isInPlay());
    }

    @Test
    public void testTeamChosenAsTheGameBegins() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Unclaimed Tanadon", ai, ZoneType.Library);
        AssertJUnit.assertNull(ChooseTeamEffect.teamOf(ai, false));
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertNotNull("a deck that cares joins a team", ChooseTeamEffect.teamOf(ai, false));
    }

    @Test
    public void testUnclaimedTanadon() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tanadon = addCardToZone("Unclaimed Tanadon", ai, ZoneType.Hand);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Phyrexian mana", tanadon.getManaCost().hasPhyrexian());
        AssertJUnit.assertEquals("same mana value", 7, tanadon.getCMC());
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(tanadon.getManaCost().hasPhyrexian());
    }

    @Test
    public void testCommandPowerPlantTron() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mine = addCard("Command Mine", ai);
        final SpellAbility mana = mine.getManaAbilities().get(0);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(mine, "X", mana));
        final Card tower = addCard("Command Tower", ai);
        addCard("Command Power Plant", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, AbilityUtils.calculateAmount(mine, "X", mana));
        AssertJUnit.assertEquals("the Tower gains the three-mana ability", 2, tower.getManaAbilities().size());
    }

    @Test
    public void testFearOfGoing02Drop() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card fear = addCard("Fear of Going 0-2 Drop", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("0-0", 0, fear.getNetPower());
        ai.getRegisteredPlayer().setEventLosses(1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("0-1 in a sealed gauntlet", 2, fear.getNetPower());
        ai.getRegisteredPlayer().setEventLosses(2);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(0, fear.getNetPower());
    }

    @Test
    public void testJoinTheWinningTeamEquipment() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card sword = addCard("Bonesplitter", human);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card join = addCardToZone("Join the Winning Team", ai, ZoneType.Hand);
        final SpellAbility sa = join.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(sword);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(ai, sword.getController());
        AssertJUnit.assertEquals(bears, sword.getAttachedTo());
        AssertJUnit.assertNotNull(ChooseTeamEffect.teamOf(ai, false));
    }

    @Test
    public void testUnknownEventShoresColor() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card shores = addCardToZone("Unknown Event Shores", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertEquals(1, shores.getMarkedColors().countColors());
    }
}
