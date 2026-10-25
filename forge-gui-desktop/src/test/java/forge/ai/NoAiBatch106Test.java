package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Second pass, batch 106: AILogic$ Untap (UntapSymbolAi), the {Q} creatures untapped once their combat is done; and
 * CountersMoveAi's two-target moves (Daghatar, Fate Transfer, Bioshift, Nesting Grounds).
 */
public class NoAiBatch106Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    // Merrow Grimeblotter: tapped from attacking, it untaps in main 2 to block, cursing their best creature
    @Test
    public void testGrimeblotterUntapsAfterCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Island", ai);
        }
        Card merrow = addCard("Merrow Grimeblotter", ai);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility sa = ability(merrow);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame("untapped, nothing to gain", AiPlayDecision.WillPlay, decide(ai, sa));

        merrow.setTapped(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame("its own combat is still ahead", AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Leech Bonder: a -1/-1 counter off itself onto their creature
    @Test
    public void testLeechBonderShipsItsCounter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        Card bonder = addCard("Leech Bonder", ai);
        bonder.addCounterInternal(CounterEnumType.M1M1, 2, ai, false, null, null);
        bonder.setTapped(true);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility sa = ability(bonder);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bonder, sa.getTargets().getFirstTargetedCard());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }

    // Daghatar the Adamant: a +1/+1 counter off their Wurm onto its own creature
    @Test
    public void testDaghatarStealsACounter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Bayou", ai);
        }
        Card daghatar = addCard("Daghatar the Adamant", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = ability(daghatar);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame("no counters anywhere", AiPlayDecision.WillPlay, decide(ai, sa));

        wurm.addCounterInternal(CounterEnumType.P1P1, 2, opp, false, null, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargets().getFirstTargetedCard());
    }

    // Fate Transfer: all of their Wurm's counters onto its Bears; one counter's worth isn't worth the card
    @Test
    public void testFateTransferTakesTheirCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Bayou", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Fate Transfer", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        wurm.addCounterInternal(CounterEnumType.P1P1, 3, opp, false, null, null);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargets().getFirstTargetedCard());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }

    // Bioshift: in answer to a Doom Blade on its pumped Giant, the counters go to the Bears
    @Test
    public void testBioshiftSavesTheCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Bayou", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        giant.addCounterInternal(CounterEnumType.P1P1, 3, ai, false, null, null);
        SpellAbility sa = addCardToZone("Bioshift", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame("moving its own counters around gains nothing", AiPlayDecision.WillPlay, decide(ai, sa));

        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(giant);
        game.getStack().add(blade);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargets().getFirstTargetedCard());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }

    // Nesting Grounds: the -1/-1 counter off its Bears onto their Wurm
    @Test
    public void testNestingGroundsShipsADebuff() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Bayou", ai);
        Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.M1M1, 1, ai, false, null, null);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = ability(addCard("Nesting Grounds", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, sa.getSubAbility().getTargetCard());
    }
}
