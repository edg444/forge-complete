package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** DiscardForXAi: AILogic$ DiscardForX. */
public class NoAiDiscardForXTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Restless Dreams: X creature cards back, X no more than its spare lands
    @Test
    public void testRestlessDreamsTradesSpareLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 7; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        addCardToZone("Hill Giant", ai, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Restless Dreams", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        // nothing spare to discard
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCardToZone("Swamp", ai, ZoneType.Hand);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }
}
