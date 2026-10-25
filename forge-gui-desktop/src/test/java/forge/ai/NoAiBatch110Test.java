package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 110: "pay any amount of life" as it enters (StoreSVarAi.lifeToPay). */
public class NoAiBatch110Test extends AITest {

    // Minion of the Wastes: at 20 life it pays 6 and is a 6/6
    @Test
    public void testMinionPaysHalfAboveEight() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card minion = addCardToZone("Minion of the Wastes", ai, ZoneType.Hand);
        Card played = game.getAction().moveToPlay(minion, null, null);
        AssertJUnit.assertEquals(14, ai.getLife());
        AssertJUnit.assertEquals(6, played.getNetToughness());
    }

    // Minion of the Wastes: at 10 life there's nothing to pay with, so it isn't cast
    @Test
    public void testMinionNotCastWhenLifeIsShort() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        SpellAbility sa = addCardToZone("Minion of the Wastes", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());

        ai.setLife(10, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());
    }

    // Nameless Race: capped by their white permanents, here two
    @Test
    public void testNamelessRaceWithinItsCap() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Savannah Lions", opp);
        addCard("Serra Angel", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card race = addCardToZone("Nameless Race", ai, ZoneType.Hand);
        // the cap is counted on the last-known battlefield, which a real game keeps current
        game.copyLastState();
        Card played = game.getAction().moveToPlay(race, null, null);
        AssertJUnit.assertEquals(18, ai.getLife());
        AssertJUnit.assertEquals(2, played.getNetPower());
    }
}
