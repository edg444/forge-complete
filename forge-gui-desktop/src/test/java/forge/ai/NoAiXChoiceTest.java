package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** XChoiceAi: AILogic$ StealX and WipeX. */
public class NoAiXChoiceTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Entrancing Melody: X exactly the mana value of the best creature it can pay for
    @Test
    public void testEntrancingMelodyPaysForTheAngel() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 7; i++) {
            addCard("Island", ai);
        }
        addCard("Grizzly Bears", opp);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Entrancing Melody", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(5, sa.getXManaCostPaid().intValue());
    }

    // Forced March: X high enough for their creatures, low enough to spare its own Craw Wurm
    @Test
    public void testForcedMarchSparesItsOwn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 9; i++) {
            addCard("Swamp", ai);
        }
        addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        addCard("Llanowar Elves", opp);
        SpellAbility sa = addCardToZone("Forced March", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(4, sa.getXManaCostPaid().intValue());
    }
}
