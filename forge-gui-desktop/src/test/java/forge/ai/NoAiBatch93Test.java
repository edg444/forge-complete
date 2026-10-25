package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 93. */
public class NoAiBatch93Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Midnight Ritual: X creature cards exiled for X Zombies, as many as it can pay for
    @Test
    public void testMidnightRitualPicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 6; i++) {
            addCard("Swamp", ai);
        }
        for (int i = 0; i < 5; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        }
        SpellAbility sa = addCardToZone("Midnight Ritual", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(3, sa.getXManaCostPaid().intValue());
        AssertJUnit.assertEquals(3, sa.getTargets().size());
    }

    // Ensnare: everything tapped as their combat begins, against a lethal attack
    @Test
    public void testEnsnareBeforeLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Island", ai);
        }
        Card wurm = addCard("Craw Wurm", opp);
        wurm.setSickness(false);
        ai.setLife(5, null);
        SpellAbility sa = addCardToZone("Ensnare", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, opp);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        ai.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
