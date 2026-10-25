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

/** Second pass, batch 92. */
public class NoAiBatch92Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Overmaster: a sorcery cantrip, cast in its second main phase
    @Test
    public void testOvermasterInMainTwo() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Mountain", ai);
        SpellAbility sa = addCardToZone("Overmaster", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Polymorphist's Jest: their two blockers become 1/1s its attackers kill
    @Test
    public void testPolymorphistsJestOnTheirBlockers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Island", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        Card wurm = addCard("Craw Wurm", opp);
        Card wall = addCard("Wall of Stone", opp);
        SpellAbility sa = addCardToZone("Polymorphist's Jest", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addAttacker(giant, opp);
        combat.addBlocker(bears, wurm);
        combat.addBlocker(giant, wall);
        combat.setBlocked(bears, true);
        combat.setBlocked(giant, true);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Hunter's Insight: its unblocked Craw Wurm draws six
    @Test
    public void testHuntersInsightOnTheUnblocked() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        Card wurm = addCard("Craw Wurm", ai);
        SpellAbility sa = addCardToZone("Hunter's Insight", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(wurm, opp);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }
}
