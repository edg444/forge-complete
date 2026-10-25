package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 94. */
public class NoAiBatch94Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Reprocess: the lands it doesn't need, a card for each
    @Test
    public void testReprocessSacrificesSpareLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 9; i++) {
            addCard("Swamp", ai);
        }
        addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Reprocess", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        // nine lands, five needed: four of them, never the Angel
        AssertJUnit.assertEquals(4, SacrificePayoffAi.planned(sa).size());
        AssertJUnit.assertTrue(SacrificePayoffAi.planned(sa).stream().allMatch(c -> c.isLand()));
    }

    // Malicious Advice: their one blocker tapped, and the attack is lethal
    @Test
    public void testMaliciousAdviceTapsTheBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        addCard("Hill Giant", ai).setSickness(false);
        forge.game.card.Card wall = addCard("Wall of Stone", opp);
        opp.setLife(3, null);
        SpellAbility sa = addCardToZone("Malicious Advice", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(1, sa.getXManaCostPaid().intValue());
        AssertJUnit.assertEquals(wall, sa.getTargetCard());
    }

    // ...and not with nothing spare
    @Test
    public void testReprocessNotForNothing() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Reprocess", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
