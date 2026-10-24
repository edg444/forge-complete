package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Name-a-card spells: AILogic names a card from the right deck, and Demonic Consultation only digs for one it can find. */
public class NoAiNameCardTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void cards(Player p, String name, ZoneType zone, int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, zone);
        }
    }

    // Cabal Therapy: "Choose a nonland card name. Target player reveals their hand and discards all cards with that name."
    @Test
    public void testCabalTherapyNamesTheOpponentsCommonSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Swamp", ai);
        cards(opp, "Lightning Bolt", ZoneType.Library, 4);
        cards(opp, "Mountain", ZoneType.Library, 8);
        SpellAbility sa = addCardToZone("Cabal Therapy", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Lightning Bolt",
                ai.getController().chooseCardName(sa, f -> true, sa.getParam("ValidCards"), ""));
    }

    // Demonic Consultation: "Choose a card name. Exile the top six cards of your library, then reveal cards ... until you
    // reveal a card with the chosen name. Put that card into your hand and exile all other cards revealed this way."
    @Test
    public void testDemonicConsultationNeedsCopiesToFind() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Swamp", ai);
        cards(ai, "Swamp", ZoneType.Library, 20);
        cards(ai, "Hill Giant", ZoneType.Library, 2);
        SpellAbility sa = addCardToZone("Demonic Consultation", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        cards(ai, "Hill Giant", ZoneType.Library, 2);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Hill Giant", ai.getController().chooseCardName(sa, f -> true, "", ""));
    }
}
