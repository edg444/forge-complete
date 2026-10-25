package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 103. */
public class NoAiBatch103Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Crush of Tentacles: their board of creatures back to hand, its own empty
    @Test
    public void testCrushOfTentaclesOnTheirBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Island", ai);
        }
        addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Crush of Tentacles", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Aethersnatch: their Craw Wurm, cast, becomes its own
    @Test
    public void testAethersnatchStealsACreatureSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Island", ai);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility wurm = addCardToZone("Craw Wurm", opp, ZoneType.Hand).getFirstSpellAbility();
        wurm.setActivatingPlayer(opp);
        game.getStack().add(wurm);
        SpellAbility sa = addCardToZone("Aethersnatch", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertSame(wurm, sa.getTargets().getFirstTargetedSpell());
    }
}
