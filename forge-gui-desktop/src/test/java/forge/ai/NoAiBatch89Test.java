package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 89. */
public class NoAiBatch89Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Moment of Silence: before an attack that could be lethal, they skip combat
    @Test
    public void testMomentOfSilenceBeforeLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        Card wurm = addCard("Craw Wurm", opp);
        wurm.setSickness(false);
        SpellAbility sa = addCardToZone("Moment of Silence", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        ai.setLife(6, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Orcish Settlers: {X}{X}{R} - two of their lands with five mana to spare
    @Test
    public void testOrcishSettlersPaysTwicePerLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Mountain", ai);
        }
        Card settlers = addCard("Orcish Settlers", ai);
        settlers.setSickness(false);
        for (int i = 0; i < 4; i++) {
            addCard("Forest", opp);
        }
        SpellAbility sa = null;
        for (SpellAbility s : settlers.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
    }
}
