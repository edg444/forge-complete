package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 86. */
public class NoAiBatch86Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Death Bomb: its token-cheap Grizzly Bears for their Serra Angel
    @Test
    public void testDeathBombSacrificesTheLeast() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Craw Wurm", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Death Bomb", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(bears, SacrificePayoffAi.planned(sa).get(0));
    }

    // Dregs of Sorrow: X is what it can pay for, among their creatures worth it
    @Test
    public void testDregsOfSorrowPicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 7; i++) {
            addCard("Swamp", ai);
        }
        addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Dregs of Sorrow", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }

    // Echoing Ruin: the artifact with the most copies of theirs
    @Test
    public void testEchoingRuinHitsTheCopies() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        addCard("Ornithopter", opp);
        addCard("Ornithopter", opp);
        addCard("Ornithopter", opp);
        SpellAbility sa = addCardToZone("Echoing Ruin", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Ornithopter", sa.getTargetCard().getName());
    }
}
