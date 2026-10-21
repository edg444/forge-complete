package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Point to the Scoreboard through Blended Twistling (UNK CU11-CZ03), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch8Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testPointToTheScoreboard() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        final Card bears = addCard("Grizzly Bears", human);
        bears.setTapped(true);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card point = addCardToZone("Point to the Scoreboard", ai, ZoneType.Hand);
        final SpellAbility sa = point.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals(1, ChooseTeamEffect.teamPoints(ai, ChooseTeamEffect.PHYREXIAN));
    }

    @Test
    public void testChicSophisticated() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card longName = addCard("Gnarlwood Dryad", ai);
        final Card shortName = addCard("Grizzly Bears", ai);
        final Card fifteen = addCard("Benalish Marshal", ai);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card chic = addCardToZone("Chic // Ago", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(chic.hasKeyword(Keyword.FUSE));
        AssertJUnit.assertFalse("14 letters", longName.isValid("Creature.sophisticated", ai, chic, null));
        AssertJUnit.assertFalse(shortName.isValid("Creature.sophisticated", ai, chic, null));
        AssertJUnit.assertTrue("15 letters", fifteen.isValid("Creature.sophisticated", ai, chic, null));
    }

    @Test
    public void testIlluminatingDetectiveWithoutCommanders() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card detective = addCard("Illuminating Detective", ai);
        final SpellAbility etb = detective.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("no commanders: already revealed", 1, detective.getCounters(forge.game.card.CounterType.getType("Flying")));
    }

    @Test
    public void testUnclaimedCat() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card cat = addCard("Unclaimed Cat", ai);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(cat.hasKeyword(Keyword.LIFELINK));
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(cat.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue(cat.hasKeyword(Keyword.TOXIC));
    }

    @Test
    public void testCatchUpMechanicModes() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card mechanic = addCard("Catch-Up Mechanic", ai);
        final SpellAbility charm = mechanic.getTriggers().get(0).ensureAbility();
        charm.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("choose one", 1, AbilityUtils.calculateAmount(mechanic, "X", charm));
        ai.setLife(14, null);
        AssertJUnit.assertEquals("an opponent has 6 more life: any number", 3, AbilityUtils.calculateAmount(mechanic, "X", charm));
        AssertJUnit.assertEquals(20, human.getLife());
    }

    @Test
    public void testDriveToWork() throws Exception {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card drive = addCardToZone("Drive to Work", ai, ZoneType.Hand);
        final SpellAbility sa = drive.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, ai);
        final java.lang.reflect.Method begin = game.getPhaseHandler().getClass().getDeclaredMethod("onPhaseBegin");
        begin.setAccessible(true);
        begin.invoke(game.getPhaseHandler());
        resolveAll(game);
        final Card back = game.getCardState(bears);
        AssertJUnit.assertTrue("returned at the end step", back.isInPlay());
        AssertJUnit.assertEquals("under its owner's control", human, back.getController());
    }

    @Test
    public void testImGoingOnAnAdventure() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card adventure = addCardToZone("I'm Going On An Adventure!", ai, ZoneType.Hand);
        final SpellAbility sa = adventure.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        final Card exiled = game.getCardState(bears);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse("its owner may cast it", exiled.mayPlay(human).isEmpty());
        AssertJUnit.assertTrue(exiled.mayPlay(ai).isEmpty());
        AssertJUnit.assertTrue(adventure.hasKeyword(Keyword.FLASHBACK));
    }

    @Test
    public void testSpareChangelingReturns() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spare = addCardToZone("Spare Changeling", ai, ZoneType.Graveyard);
        final Card kicked = addCardToZone("Burst Lightning", ai, ZoneType.Hand);
        SpellAbility cast = kicked.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final SpellAbility withKicker = cast.copy();
        withKicker.addOptionalCost(forge.game.spellability.OptionalCost.Kicker1);
        AssertJUnit.assertTrue(withKicker.isValid("Spell.AdditionalCostPaid", ai, spare, null));
        AssertJUnit.assertFalse(cast.isValid("Spell.AdditionalCostPaid", ai, spare, null));
        AssertJUnit.assertTrue(spare.getType().hasSubtype("Treasure"));
    }

    @Test
    public void testBlendedTwistling() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card twistling = addCard("Blended Twistling", ai);
        addCard("Boros Reckoner", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("its own {G/W} and the Reckoner's three", 2 + 4, twistling.getNetPower());
    }

    @Test
    public void testOneDoesNot() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card aura = addCard("One Does Not", ai);
        aura.attachToEntity(bears, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(bears.hasKeyword("Nonbasic landwalk"));
    }
}
