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

/** SacrificePayoffAi: AILogic$ RevealFor and ExileForX. */
public class NoAiRevealExileXTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Induce Despair: -X/-X, X the revealed creature card's mana value - the Craw Wurm in hand kills the Angel
    @Test
    public void testInduceDespairRevealsTheBigOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Induce Despair", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));
    }

    // Harvest Pyre: X damage, X cards exiled from its graveyard - just what the kill takes
    @Test
    public void testHarvestPyreExilesWhatItNeeds() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Island", ai, ZoneType.Graveyard);
        }
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Harvest Pyre", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(4, sa.getXManaCostPaid().intValue());
    }
}
