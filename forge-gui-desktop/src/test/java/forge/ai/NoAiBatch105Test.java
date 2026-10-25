package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 105: AILogic$ Shoal (ShoalAi), X from the card exiled for the alternative cost or from mana. */
public class NoAiBatch105Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility alt(Player ai, Card shoal) {
        SpellAbility sa = shoal.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return GameActionUtil.getAlternativeCosts(sa, ai, false).get(0);
    }

    private static Combat attack(Game game, Player attacker, Card att, Player defender) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        combat.setBlocked(att, false);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Sickening Shoal: no lands, so it exiles the 3-drop (not the 4-drop) to kill their 2/2
    @Test
    public void testSickeningShoalExilesTheCheapestKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", opp);
        Card shoal = addCardToZone("Sickening Shoal", ai, ZoneType.Hand);
        addCardToZone("Phyrexian Rager", ai, ZoneType.Hand);
        addCardToZone("Gravedigger", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        List<SpellAbility> play = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(play);
        SpellAbility sa = play.get(0);
        AssertJUnit.assertEquals(shoal, sa.getHostCard());
        AssertJUnit.assertEquals(Integer.valueOf(3), sa.getXManaCostPaid());
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Blazing Shoal: their 5 life less the Bears' 2 needs X of 3 or more, so the 4-drop goes
    @Test
    public void testBlazingShoalPumpsForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.setLife(5, null);
        Card bears = addCard("Grizzly Bears", ai);
        SpellAbility sa = alt(ai, addCardToZone("Blazing Shoal", ai, ZoneType.Hand));
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        attack(game, ai, bears, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
        AssertJUnit.assertEquals(bears, sa.getTargetCard());

        opp.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Disrupting Shoal: their 4-drop is countered by exiling its own 4-drop, not with a mismatched one
    @Test
    public void testDisruptingShoalMatchesTheManaValue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = alt(ai, addCardToZone("Disrupting Shoal", ai, ZoneType.Hand));
        addCardToZone("Air Elemental", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility giant = addCardToZone("Hill Giant", opp, ZoneType.Hand).getFirstSpellAbility();
        giant.setActivatingPlayer(opp);
        game.getStack().add(giant);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Nourishing Shoal: at 3 life facing an unblocked 6/4, only the 6-drop keeps it alive
    @Test
    public void testNourishingShoalSurvivesTheAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = alt(ai, addCardToZone("Nourishing Shoal", ai, ZoneType.Hand));
        addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        attack(game, opp, wurm, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(6), sa.getXManaCostPaid());

        ai.setLife(10, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Shining Shoal: the 6-drop moves all of the Wurm's 6 damage, which the 5-drop wouldn't
    @Test
    public void testShiningShoalMovesTheWholeHit() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(5, null);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = alt(ai, addCardToZone("Shining Shoal", ai, ZoneType.Hand));
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        addCardToZone("Sunblast Angel", ai, ZoneType.Hand);
        attack(game, opp, wurm, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(6), sa.getXManaCostPaid());
    }
}
