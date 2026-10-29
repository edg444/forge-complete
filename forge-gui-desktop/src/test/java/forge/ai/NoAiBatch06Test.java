package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, sixth batch (A-B): each test is the situation that card's new AI is for. */
public class NoAiBatch06Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Barter in Blood: "Each player sacrifices two creatures of their choice."
    @Test
    public void testBarterInBloodWhenTheirsAreWorthMore() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 4);
        addCard("Grizzly Bears", ai);
        addCard("Craw Wurm", opp);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Barter in Blood", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    @Test
    public void testBarterInBloodNotAnEvenTrade() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 4);
        addCard("Craw Wurm", ai);
        addCard("Craw Wurm", ai);
        addCard("Craw Wurm", opp);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Barter in Blood", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Battle Cry: "Untap all white creatures you control. Whenever a creature blocks this turn, it gets +0/+1 until end of turn."
    @Test
    public void testBattleCryUntapsAttackersAfterCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Plains", 3);
        Card a = addCard("Savannah Lions", ai);
        Card b = addCard("Savannah Lions", ai);
        a.tap(null, null);
        b.tap(null, null);
        SpellAbility sa = addCardToZone("Battle Cry", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Behold the Beyond: "Discard your hand. Search your library for three cards, put them into your hand, then shuffle."
    @Test
    public void testBeholdTheBeyondOnlyWithAFewCardsLeft() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Swamp", 7);
        fillLibrary(ai, 10);
        SpellAbility sa = addCardToZone("Behold the Beyond", ai, ZoneType.Hand).getFirstSpellAbility();
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        for (int i = 0; i < 2; i++) {
            for (Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (c.getName().equals("Grizzly Bears")) {
                    game.getAction().moveToGraveyard(c, null);
                    break;
                }
            }
        }
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Hand).contains(sa.getHostCard()));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Biorhythm: "Each player's life total becomes the number of creatures they control."
    @Test
    public void testBiorhythmForTheWinNeverIntoALoss() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Forest", 8);
        SpellAbility sa = addCardToZone("Biorhythm", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Balance of Power: "If target opponent has more cards in hand than you, draw cards equal to the difference."
    @Test
    public void testBalanceOfPowerWhenTheyHaveMoreCards() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 5);
        fillLibrary(ai, 10);
        SpellAbility sa = addCardToZone("Balance of Power", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        for (int i = 0; i < 4; i++) {
            addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        }
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }
}
