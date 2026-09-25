package forge.net;

import forge.deck.Deck;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.collect.FCollectionView;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Who // What // When // Where // Why over real network play. Its third to fifth faces live in
 * the fork's Split3State-Split5State slots, which the client has to decode back into state views
 * exactly like the two standard split slots, or the remote client sees raw state numbers.
 */
public class FiveFaceSplitNetworkTest {

    private static final String FIVE_FACE_SPLIT = "Who // What // When // Where // Why";

    @BeforeClass
    public static void setUp() {
        TestUtils.ensureFModelInitialized();
    }

    @Test(timeOut = 90000)
    public void testRemoteClientSeesAllFiveSplitFaces() {
        PaperCard card = FModel.getMagicDb().getCommonCards().getCard(FIVE_FACE_SPLIT);
        Assert.assertNotNull(card, FIVE_FACE_SPLIT + " should be in the card database");
        Deck deck1 = new Deck("Five-face x10");
        Deck deck2 = new Deck("Five-face x10");
        for (int i = 0; i < 10; i++) {
            deck1.getMain().add(card);
            deck2.getMain().add(card);
        }

        UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                .playerCount(2)
                .remoteClients(1)
                .decks(deck1, deck2)
                .gameTimeout(60000)
                .execute();

        Assert.assertTrue(result.gameStarted, "Game should have started: " + result.toSummary());
        GameView clientGameView = result.clientGameView;
        Assert.assertNotNull(clientGameView, "Client should have a GameView");

        List<CardView> fiveFaceCards = new ArrayList<>();
        ZoneType[] zones = {ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Library};
        for (PlayerView pv : clientGameView.getPlayers()) {
            for (ZoneType zone : zones) {
                FCollectionView<CardView> cards = pv.getCards(zone);
                if (cards == null) continue;
                for (CardView cv : cards) {
                    if (cv.isSplitCard() && cv.getLeftSplitState() != null) {
                        fiveFaceCards.add(cv);
                    }
                }
            }
        }
        Assert.assertFalse(fiveFaceCards.isEmpty(), "Client should see at least one split card in full");

        for (CardView cv : fiveFaceCards) {
            List<CardStateView> faces = cv.getSplitStates();
            List<String> names = new ArrayList<>();
            for (CardStateView face : faces) {
                names.add(face.getName());
            }
            Assert.assertEquals(names, List.of("Who", "What", "When", "Where", "Why"),
                    "CardView id=" + cv.getId() + " split faces on the client");
        }
        Assert.assertEquals(result.sendErrors, 0, "Server send errors");
    }
}
