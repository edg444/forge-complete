package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, batches 20-25 (D-F): each test is the situation that card's new AI is for. */
public class NoAiBatch25Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Fireblade Artist: "At the beginning of your upkeep, you may sacrifice a creature. When you do, ..."
    @Test
    public void testOptionalSacrificeOnlyOfASpareCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card artist = addCard("Fireblade Artist", ai);
        addCard("Grizzly Bears", ai);
        SpellAbility sac = null;
        for (Trigger t : artist.getTriggers()) {
            sac = t.ensureAbility();
        }
        sac.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sac).confirmAction(ai, sac, null, "", null));
        Card token = addToken("c_1_1_a_thopter_flying", ai);
        token.setSVar("SacMe", "5");
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sac).confirmAction(ai, sac, null, "", null));
    }

    // Final Fortune: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
    @Test
    public void testFinalFortuneOnlyWhenTheExtraTurnWins() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 2);
        addCard("Hill Giant", ai);
        SpellAbility sa = addCardToZone("Final Fortune", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(3, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
