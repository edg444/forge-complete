package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.PactAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** BlinkAi (AILogic$ Blink) and PactAi (pacts are only cast when next upkeep's payment is there). */
public class NoAiBlinkPactTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Ghostly Flicker: "Exile two target artifacts, creatures, and/or lands you control, then return those cards" - in
    // answer to removal, with a land as the second target
    @Test
    public void testGhostlyFlickerSavesFromRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        Card angel = addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Ghostly Flicker", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);

        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(angel);
        game.getStack().add(blade);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(angel));
    }

    // ...but not for nothing
    @Test
    public void testGhostlyFlickerNotForNothing() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Island", 3);
        addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Ghostly Flicker", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Pact of Negation: {3}{U}{U} next upkeep - five lands aren't enough without two blue sources
    @Test
    public void testPactOfNegationNeedsTheBlue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Island", ai);
        lands(ai, "Mountain", 4);
        SpellAbility sa = addCardToZone("Pact of Negation", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertFalse(PactAi.canPayLater(ai, sa));
        addCard("Island", ai);
        AssertJUnit.assertTrue(PactAi.canPayLater(ai, sa));
    }
}
