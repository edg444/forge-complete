package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 112: AILogic$ LandAlpha and GiveBack (LandAlphaGiveBackAi). */
public class NoAiBatch112Test extends AITest {

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

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p).setSickness(false);
        }
    }

    // Sylvan Awakening: eight Forests less three for the spell are five 2/2s, 10 damage past one blocker at 8 life
    @Test
    public void testSylvanAwakeningForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 8);
        addCard("Grizzly Bears", opp);
        opp.setLife(8, null);
        SpellAbility sa = addCardToZone("Sylvan Awakening", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Natural Affinity: their lands animate too and block, so the same board isn't lethal against their four lands
    @Test
    public void testNaturalAffinityCountsTheirLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 8);
        opp.setLife(8, null);
        SpellAbility sa = addCardToZone("Natural Affinity", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        lands(opp, "Plains", 4);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Nullmage Advocate: two of their graveyard lands back for their Icy Manipulator
    @Test
    public void testNullmageTradesLandsForTheirArtifact() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card nullmage = addCard("Nullmage Advocate", ai);
        nullmage.setSickness(false);
        addCardToZone("Swamp", opp, ZoneType.Graveyard);
        addCardToZone("Swamp", opp, ZoneType.Graveyard);
        addCardToZone("Serra Angel", opp, ZoneType.Graveyard);
        SpellAbility sa = ability(nullmage);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame("nothing to destroy", AiPlayDecision.WillPlay, decide(ai, sa));

        Card icy = addCard("Icy Manipulator", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(icy, sa.getSubAbility().getTargetCard());
        AssertJUnit.assertFalse(sa.getTargets().getTargetCards().stream().anyMatch(c -> c.getName().equals("Serra Angel")));
    }

    // Pulsemage Advocate: three of their lands back for its Craw Wurm
    @Test
    public void testPulsemageReanimates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card pulse = addCard("Pulsemage Advocate", ai);
        pulse.setSickness(false);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Plains", opp, ZoneType.Graveyard);
        }
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        SpellAbility sa = ability(pulse);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getSubAbility().getTargetCard());
    }
}
