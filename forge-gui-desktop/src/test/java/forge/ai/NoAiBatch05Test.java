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

/** AI:RemoveDeck:All cards, fifth batch (A...): each test is the situation that card's new AI is for. */
public class NoAiBatch05Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Artillerize: "As an additional cost to cast this spell, sacrifice an artifact or creature. Artillerize deals 5 damage to any target."
    @Test
    public void testArtillerizeSacrificesATokenForTheWin() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 4);
        opp.setLife(5, null);
        SpellAbility sa = addCardToZone("Artillerize", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addToken("c_1_1_a_thopter_flying", ai);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Ashling the Pilgrim: "{1}{R}: Put a +1/+1 counter on Ashling. If this is the third time this ability has resolved
    // this turn, remove all +1/+1 counters from Ashling, and it deals that much damage to each creature and each player."
    @Test
    public void testAshlingStopsBeforeTheThirdUnlessItWins() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 6);
        Card ashling = addCard("Ashling the Pilgrim", ai);
        ashling.addCounterInternal(CounterEnumType.P1P1, 3, ai, false, null, null);
        SpellAbility sa = activated(ashling);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        ashling.addAbilityActivated(sa);
        ashling.addAbilityActivated(sa);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(4, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
