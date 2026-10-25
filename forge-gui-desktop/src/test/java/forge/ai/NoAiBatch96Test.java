package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 96. */
public class NoAiBatch96Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Burning Cloak: two damage to their Grizzly Bears, not the pump
    @Test
    public void testBurningCloakAsRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mountain", ai);
        Card bears = addCard("Grizzly Bears", opp);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Burning Cloak", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Fire and Brimstone: only for the win
    @Test
    public void testFireAndBrimstoneForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        SpellAbility sa = addCardToZone("Fire and Brimstone", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        opp.addCreaturesAttackedThisTurn(addCard("Grizzly Bears", opp), ai);
        opp.setLife(4, null);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(9, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
