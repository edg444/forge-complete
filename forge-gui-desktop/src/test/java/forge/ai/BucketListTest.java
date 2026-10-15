package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Bucket List (CMB1 #89), checked against Scryfall and its 5 rulings 2026-10-15: a spell triggers it only with a type
 * still showing, covers every one of those it has, and the fifth covered type sacrifices it for one more card.
 */
public class BucketListTest extends AITest {

    private static void castAndResolve(final Game game, final Player p, final String name) {
        Card c = addCardToZoneStatic(p, name);
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(name, ComputerUtil.handlePlayingSpellAbility(p, sa, null));
        for (int i = 0; i < 6 && !game.getStack().isEmpty(); i++) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static Card addCardToZoneStatic(final Player p, final String name) {
        return p.getZone(ZoneType.Hand).getCards().stream().filter(c -> c.getName().equals(name)).findFirst().get();
    }

    @Test
    public void coverEachTypeOnceThenCashIn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        fillLibrary(ai, 20);
        addCards("Plains", 6, ai);
        addCards("Island", 6, ai);
        addCards("Forest", 6, ai);
        Card list = addCard("Bucket List", ai);
        for (String n : new String[] {"Grizzly Bears", "Llanowar Elves", "Ornithopter", "Glorious Anthem", "Opt", "Divination"}) {
            addCardToZone(n, ai, ZoneType.Hand);
        }
        game.getAction().checkStateEffects(true);

        castAndResolve(game, ai, "Grizzly Bears");
        AssertJUnit.assertEquals(1, list.getCoveredTrackerTypes().size());
        int hand = ai.getCardsIn(ZoneType.Hand).size();

        castAndResolve(game, ai, "Llanowar Elves");
        AssertJUnit.assertEquals("creature's covered: no trigger, no card", hand - 1, ai.getCardsIn(ZoneType.Hand).size());

        castAndResolve(game, ai, "Ornithopter");
        AssertJUnit.assertTrue("an artifact creature still shows artifact", list.getCoveredTrackerTypes().contains("Artifact"));
        castAndResolve(game, ai, "Glorious Anthem");
        castAndResolve(game, ai, "Opt");
        AssertJUnit.assertEquals(4, list.getCoveredTrackerTypes().size());
        AssertJUnit.assertTrue(list.isInPlay());

        hand = ai.getCardsIn(ZoneType.Hand).size();
        castAndResolve(game, ai, "Divination");
        AssertJUnit.assertFalse("sacrificed", game.getCardState(list).isInPlay());
        // Divination's 2, the trigger's 1 and one more - and Divination itself left the hand
        AssertJUnit.assertEquals(hand - 1 + 2 + 1 + 1, ai.getCardsIn(ZoneType.Hand).size());
    }
}
