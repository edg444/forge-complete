package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.NumberGuessAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 117: NumberGuessAi (Void, Blood Oath, Fertile Imagination, Sanctum Prelate). */
public class NoAiBatch117Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Void: 2 takes their two Bears, and leaves its Serra Angel
    @Test
    public void testVoidNamesTheirManaValue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Serra Angel", ai);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Void", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(2), NumberGuessAi.plannedNumber(sa));
    }

    // Blood Oath: they've shown lands, five cards in hand should be a few of them, at their end step
    @Test
    public void testBloodOathGuessesLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Mountain", opp);
            addCardToZone("Hill Giant", opp, ZoneType.Hand);
        }
        addCardToZone("Lightning Bolt", opp, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Blood Oath", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Land", NumberGuessAi.plannedType(sa));
    }

    // Fertile Imagination: its second main phase, four cards in their hand of a land-heavy deck
    @Test
    public void testFertileImaginationInMainTwo() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Forest", opp);
            addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        }
        SpellAbility sa = addCardToZone("Fertile Imagination", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Sanctum Prelate: their graveyard of Bolts and its own 2-mana spells make it name 1
    @Test
    public void testSanctumPrelateNamesTheirNumber() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Lightning Bolt", opp, ZoneType.Graveyard);
            addCardToZone("Counterspell", ai, ZoneType.Library);
        }
        AssertJUnit.assertEquals(1, NumberGuessAi.prelateNumber(ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card prelate = game.getAction().moveToPlay(addCardToZone("Sanctum Prelate", ai, ZoneType.Hand), null, null);
        AssertJUnit.assertEquals(Integer.valueOf(1), prelate.getChosenNumber());
    }
}
