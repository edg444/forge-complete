package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** PhasesAi targeting (it never picked a target before) and LifeExchangeAi's two-target and Psychic Transfer cases. */
public class NoAiPhaseLifeTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Reality Ripple: their Craw Wurm, unblocked, phases out of combat
    @Test
    public void testRealityRippleTakesAnAttackerOut() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Reality Ripple", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Reality Ripple: its own creature, away from a Doom Blade
    @Test
    public void testRealityRippleSavesFromRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        Card angel = addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Reality Ripple", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(angel);
        game.getStack().add(blade);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Soul Conduit: "Two target players exchange life totals." - itself and the opponent far ahead
    @Test
    public void testSoulConduitTargetsBothPlayers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Plains", ai);
        }
        Card conduit = addCard("Soul Conduit", ai);
        SpellAbility sa = null;
        for (SpellAbility s : conduit.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        ai.setLife(4, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }

    // Psychic Transfer: only within 5 life of each other
    @Test
    public void testPsychicTransferWithinFive() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        SpellAbility sa = addCardToZone("Psychic Transfer", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        ai.setLife(10, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        ai.setLife(16, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
