package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 113: AILogic$ TargetX (TargetXAi), X worked out for each target. */
public class NoAiBatch113Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static void blocks(Game game, Player attacker, Card att, Player defender, Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        combat.addBlocker(att, blocker);
        combat.setBlocked(att, true);
        game.getPhaseHandler().setCombat(combat);
    }

    // Weight of Spires: their Craw Wurm dies to their four nonbasic lands, not to two
    @Test
    public void testWeightOfSpiresCountsTheirNonbasics() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mountain", ai);
        Card wurm = addCard("Craw Wurm", opp);
        addCard("Bayou", opp);
        addCard("Bayou", opp);
        SpellAbility sa = addCardToZone("Weight of Spires", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Bayou", opp);
        addCard("Bayou", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Might of the Nephilim: its green Bears blocking a Hill Giant get +2/+2, live and kill it
    @Test
    public void testMightOfTheNephilimWinsTheFight() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Forest", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Might of the Nephilim", ai, ZoneType.Hand).getFirstSpellAbility();
        blocks(game, opp, giant, ai, bears);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Great Defender: +0/+5 keeps its Serra Angel alive blocking a Craw Wurm
    @Test
    public void testGreatDefenderSavesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Great Defender", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        blocks(game, opp, wurm, ai, angel);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Parallectric Feedback: 6 to the face off their Craw Wurm spell; their 1-drop isn't worth it
    @Test
    public void testParallectricFeedbackOnABigSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        SpellAbility sa = addCardToZone("Parallectric Feedback", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility elves = addCardToZone("Llanowar Elves", opp, ZoneType.Hand).getFirstSpellAbility();
        elves.setActivatingPlayer(opp);
        game.getStack().add(elves);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        SpellAbility wurm = addCardToZone("Craw Wurm", opp, ZoneType.Hand).getFirstSpellAbility();
        wurm.setActivatingPlayer(opp);
        game.getStack().add(wurm);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
