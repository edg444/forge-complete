package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 120: AILogic$ Polymorph (Mass Polymorph, Synthetic Destiny, Reweave). */
public class NoAiBatch120Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void wurms(Player ai) {
        for (int i = 0; i < 5; i++) {
            addCardToZone("Craw Wurm", ai, ZoneType.Library);
        }
    }

    // Mass Polymorph: three Elves for three of the Wurms its library is full of, in its second main phase
    @Test
    public void testMassPolymorphUpgrades() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Llanowar Elves", ai);
        }
        wurms(ai);
        SpellAbility sa = addCardToZone("Mass Polymorph", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Synthetic Destiny: in answer to their Wrath of God, its creatures come back as others
    @Test
    public void testSyntheticDestinyDodgesAWrath() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Serra Angel", ai);
        wurms(ai);
        SpellAbility sa = addCardToZone("Synthetic Destiny", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        SpellAbility wrath = addCardToZone("Wrath of God", opp, ZoneType.Hand).getFirstSpellAbility();
        wrath.setActivatingPlayer(opp);
        game.getStack().add(wrath);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Reweave: its Llanowar Elves into one of the Wurms
    @Test
    public void testReweaveItsWeakest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card elves = addCard("Llanowar Elves", ai);
        addCard("Serra Angel", ai);
        wurms(ai);
        SpellAbility sa = addCardToZone("Reweave", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(elves, sa.getTargetCard());
    }
}
