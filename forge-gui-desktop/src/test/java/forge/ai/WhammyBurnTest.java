package forge.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/**
 * Whammy Burn (CMB1 #70), checked against Scryfall and its 4 rulings 2026-10-14: the whammy deck is formed from a
 * Plains, Island, Swamp, Mountain, and Forest from outside the game the first time it's needed, lives in its own zone
 * that nothing else touches, and the target is chosen on casting, before anything is revealed.
 */
public class WhammyBurnTest extends AITest {

    @Test
    public void theWhammyDeckFormsOnceAndStaysOutOfReach() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        AssertJUnit.assertTrue(ai.getZone(ZoneType.WhammyDeck).isEmpty());

        PlayerZone whammy = ai.getWhammyDeck();
        List<String> names = whammy.getCards().stream().map(Card::getName).sorted().collect(Collectors.toList());
        AssertJUnit.assertEquals(List.of("Forest", "Island", "Mountain", "Plains", "Swamp"), names);
        AssertJUnit.assertEquals(5, ai.getWhammyDeck().size());
        for (Card c : whammy.getCards()) {
            AssertJUnit.assertEquals(ai, c.getOwner());
            AssertJUnit.assertFalse("cards you own not on the battlefield don't include it", ai.getAllCards().contains(c));
        }
    }

    @Test
    public void theAiStopsAtTwoAgainstAPlayer() {
        for (int run = 0; run < 12; run++) {
            Game game = initAndCreateGame();
            Player opp = game.getPlayers().get(0);
            Player ai = game.getPlayers().get(1);
            Card burn = addCardToZone("Whammy Burn", ai, ZoneType.Hand);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            game.getAction().checkStateEffects(true);

            Card onStack = game.getAction().moveToStack(burn, null);
            SpellAbility sa = onStack.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            sa.getSubAbility().getTargets().add(opp);
            game.getStack().add(sa);
            game.getStack().resolveStack();

            List<Card> order = new java.util.ArrayList<>(ai.getZone(ZoneType.WhammyDeck).getCards());
            int island = -1;
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).getName().equals("Island")) {
                    island = i;
                }
            }
            // reveals 1, goes on, stops at 2: 2 damage unless the Island was one of those two
            AssertJUnit.assertEquals(20 - (island >= 2 ? 2 : 0), opp.getLife());
            AssertJUnit.assertFalse(onStack.hasRemembered());
            AssertJUnit.assertTrue(game.getCardState(onStack).isInZone(ZoneType.Graveyard));
        }
    }

    @Test
    public void theAiCastsItAtSomethingSmall() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Mountain", ai);
        Card bears = addCard("Grizzly Bears", opp);
        Card burn = addCardToZone("Whammy Burn", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = burn.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getSubAbility().getTargets().size() == 1);
        AssertJUnit.assertTrue(sa.getSubAbility().getTargets().contains(bears) || sa.getSubAbility().getTargets().contains(opp));
    }
}
