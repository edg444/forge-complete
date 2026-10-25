package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.ZonePickAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 133: the fork's Un-cards, holiday cards and playtest cards. */
public class NoAiBatch133Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static Combat combat(Game game, Player attacker, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, attacker, false, 1);
        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    private static ApiType firstMode(SpellAbility charm) {
        return charm.getSubAbility() == null ? null : charm.getSubAbility().getApi();
    }

    // Evil Presents: the Craw Wurm in hand, given to them to attack them every turn
    @Test
    public void testEvilPresentsGivesABigCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Evil Presents", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
        AssertJUnit.assertTrue(ZonePickAi.planned(sa));
    }

    // Evil Presents: a 1/1 isn't worth a card
    @Test
    public void testEvilPresentsNotWithASmallCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Evil Presents", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // That's No Moonmist: their lethal attack is fogged; a small one isn't
    @Test
    public void testMoonmistFogsALethalAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(4, null);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("That's No Moonmist", ai, ZoneType.Hand).getFirstSpellAbility();
        Combat c = combat(game, opp, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(wurm, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    @Test
    public void testMoonmistNotOnASmallAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card elves = addCard("Llanowar Elves", opp);
        SpellAbility sa = addCardToZone("That's No Moonmist", ai, ZoneType.Hand).getFirstSpellAbility();
        Combat c = combat(game, opp, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(elves, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Avatar of Me: an ordinary 25-year-old, 5 1/2 feet tall with size 10 shoes
    @Test
    public void testAvatarOfMeOrdinaryFacts() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card avatar = addCardToZone("Avatar of Me", ai, ZoneType.Hand);
        SpellAbility height = AbilityFactory.getAbility(avatar.getSVar("AskHeight"), avatar);
        SpellAbility shoe = AbilityFactory.getAbility(avatar.getSVar("AskShoe"), avatar);
        height.setActivatingPlayer(ai);
        shoe.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(11, ai.getController().chooseNumber(height, "height", 0, 16));
        AssertJUnit.assertEquals(20, ai.getController().chooseNumber(shoe, "shoe", 0, 40));
        AssertJUnit.assertEquals(25, ai.askPersonalFact("Age", "age", 0, 120));
    }

    // Yet Another Night in Vegas: never the "go again" that loses 5 when that's its life
    @Test
    public void testYetAnotherNightNotGoAgainAtFiveLife() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        ai.setLife(5, null);
        Card vegas = addCard("Yet Another Night in Vegas", ai);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        SpellAbility charm = vegas.getTriggers().getFirst().ensureAbility();
        charm.setActivatingPlayer(ai);
        CharmEffect.makeChoices(charm);
        AssertJUnit.assertNotSame(ApiType.Cleanup, firstMode(charm));
    }

    // Another Night in Vegas: its Craw Wurm back from the graveyard
    @Test
    public void testAnotherNightReturnsACreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        Card vegas = addCard("Another Night in Vegas", ai);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        SpellAbility charm = vegas.getTriggers().getFirst().ensureAbility();
        charm.setActivatingPlayer(ai);
        CharmEffect.makeChoices(charm);
        AssertJUnit.assertEquals(ApiType.ChangeZone, firstMode(charm));
        AssertJUnit.assertEquals(wurm, charm.getSubAbility().getTargetCard());
    }

    // Chaos Wrap: their Serra Angel, not its own
    @Test
    public void testChaosWrapOnTheirBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Chaos Wrap", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }
}
