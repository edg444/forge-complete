package forge.ai;

import java.util.List;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.cost.CostExileFromOutside;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * Greatest Show in the Multiverse (UNF #189), checked against Scryfall and its rulings 2026-10-21: five instant and/or
 * sorcery cards with the same artist and different names, from anywhere outside the game.
 */
public class GreatestShowInTheMultiverseTest extends AITest {

    @Test
    public void testArtists() {
        initAndCreateGame();
        final Map<String, List<PaperCard>> artists = CostExileFromOutside.artists(5);
        AssertJUnit.assertTrue(artists.size() > 50);
        for (final List<PaperCard> spells : artists.values()) {
            AssertJUnit.assertEquals("different names", spells.size(), spells.stream().map(PaperCard::getName).distinct().count());
        }
    }

    @Test
    public void testCastAndFirstChapter() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 7; i++) {
            addCard("Island", ai);
        }
        game.getAction().checkStateEffects(true);
        final Card show = addCardToZone("Greatest Show in the Multiverse", ai, ZoneType.Hand);
        final SpellAbility sa = show.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        final List<Card> exiled = CardLists.filter(ai.getCardsIn(ZoneType.Exile), c -> c.isInstant() || c.isSorcery());
        AssertJUnit.assertEquals("five exiled as it's cast", 5, exiled.size());
        game.getStack().resolveStack();
        final Card saga = game.getCardState(show);
        AssertJUnit.assertTrue(saga.isInPlay());
        for (final Card c : exiled) {
            AssertJUnit.assertTrue("still linked to the Saga once it's on the battlefield",
                    c.isValid("Card.ExiledWithSource", ai, saga, null));
        }
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertFalse("chapter I triggered", game.getStack().isEmpty());
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }
}
