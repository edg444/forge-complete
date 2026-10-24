package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** ExchangeTradeAi: AILogic$ ExchangePair and SwapArmies. */
public class NoAiExchangeTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility trigger(Card c, Player ai) {
        for (Trigger t : c.getTriggers()) {
            SpellAbility sa = t.ensureAbility();
            if (sa != null) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("no trigger on " + c);
    }

    // Political Trickery: a basic for their Ancient Tomb, not for another basic
    @Test
    public void testPoliticalTrickeryWantsTheBetterLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card forest = addCard("Forest", ai);
        addCard("Island", ai);
        addCard("Island", ai);
        addCard("Mountain", opp);
        SpellAbility sa = addCardToZone("Political Trickery", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card tomb = addCard("Ancient Tomb", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertTrue(sa.getTargets().contains(forest) || sa.getTargetCard().getController().equals(ai));
        AssertJUnit.assertEquals(tomb, sa.getSubAbility().getTargetCard());
    }

    // Spawnbroker: "you may exchange control of target creature you control and target creature with power less than or
    // equal to that creature's power an opponent controls" - the least it can give for the best it can reach
    @Test
    public void testSpawnbrokerTradesUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card spawnbroker = addCard("Spawnbroker", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card nighthawk = addCard("Vampire Nighthawk", opp);
        addCard("Serra Angel", opp);
        SpellAbility sa = trigger(spawnbroker, ai);

        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, false));
        // the Bears (power 2) reach the Nighthawk but not the Angel (power 4)
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
        AssertJUnit.assertEquals(nighthawk, sa.getSubAbility().getTargetCard());
    }

    // Reins of Power: their army into its own blockers, when that's lethal and its own attack isn't
    @Test
    public void testReinsOfPowerForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Island", ai);
        }
        Card wall = addCard("Wall of Stone", ai);
        wall.setSickness(false);
        for (int i = 0; i < 3; i++) {
            addCard("Hill Giant", opp);
        }
        SpellAbility sa = addCardToZone("Reins of Power", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        opp.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(6, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }
}
