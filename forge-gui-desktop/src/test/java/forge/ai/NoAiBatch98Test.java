package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 98. */
public class NoAiBatch98Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Banshee: X 6 for 3 to their Hill Giant, 3 to itself
    @Test
    public void testBansheePaysDoubleForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 7; i++) {
            addCard("Swamp", ai);
        }
        Card banshee = addCard("Banshee", ai);
        banshee.setSickness(false);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = null;
        for (SpellAbility s : banshee.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
        AssertJUnit.assertEquals(6, sa.getXManaCostPaid().intValue());
    }

    // Command the Dreadhorde: the creatures in graveyards worth their life
    @Test
    public void testCommandTheDreadhordePicksValue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Serra Angel", opp, ZoneType.Graveyard);
        addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Command the Dreadhorde", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }
}
