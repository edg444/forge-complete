package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** MiscLogicAi: BounceBoth, GraveShuffle, RemoveCombatant, CashIn. */
public class NoAiMiscLogicTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    // Peel from Reality: its own creature about to be Doom Bladed, and their best one, back to hand
    @Test
    public void testPeelFromRealitySavesAndBounces() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Peel from Reality", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(angel);
        game.getStack().add(blade);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, sa.getSubAbility().getTargetCard());
    }

    // Reconnaissance: its attacker blocked by something that kills it and lives comes out of combat
    @Test
    public void testReconnaissancePullsADoomedAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card recon = addCard("Reconnaissance", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, wurm);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        SpellAbility sa = activated(recon);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Midsummer Revel: cashed in at three verse counters, not before
    @Test
    public void testMidsummerRevelWaitsForCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        Card revel = addCard("Midsummer Revel", ai);
        SpellAbility sa = activated(revel);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        revel.addCounterInternal(CounterType.getType("VERSE"), 2, ai, false, null, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        revel.addCounterInternal(CounterType.getType("VERSE"), 1, ai, false, null, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
