package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 99. */
public class NoAiBatch99Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Phyrexian Purge: their two worthwhile creatures for 6 life, keeping 8 back
    @Test
    public void testPhyrexianPurgeKeepsLifeBack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Swamp", ai);
            addCard("Mountain", ai);
        }
        addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp);
        addCard("Serra Angel", opp);
        ai.setLife(14, null);
        SpellAbility sa = addCardToZone("Phyrexian Purge", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }

    // Mana Vapors: their lands, tapped now, stay tapped
    @Test
    public void testManaVaporsOnTappedLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Forest", opp).setTapped(true);
        }
        SpellAbility sa = addCardToZone("Mana Vapors", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }
}
