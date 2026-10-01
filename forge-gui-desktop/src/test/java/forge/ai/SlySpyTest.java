package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Sly Spy (Unstable), six printings, each "Whenever this creature deals combat damage to a player, ..." with its own
 * effect - Oracle text and rulings for 67a-f and the Unstable FAQ entry checked 2026-10-01.
 */
public class SlySpyTest extends AITest {

    private Card printing(String name, String cn, Player p, ZoneType zone) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, "UST", cn);
        AssertJUnit.assertNotNull(name + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    /** The AI attacks with the spy into an empty board; returns after combat. */
    private Card attack(Game game, Player ai, String cn) {
        Card spy = printing("Sly Spy", cn, ai, ZoneType.Battlefield);
        spy.setSickness(false);
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.MAIN2);
        return spy;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        String[][] texts = {
            {"67a", "that player reveals their hand. You choose a card from it with the longest name. That player discards that card."},
            {"67b", "you may destroy target creature facing left in its art. (Creatures without faces don't face anywhere.)"},
            {"67c", "that player loses a finger until this creature leaves the battlefield. (The finger is chosen by its owner and can't roll dice or touch cards.)"},
            {"67d", "you may destroy target creature facing right in its art. (Creatures without faces don't face anywhere.)"},
            {"67e", "that player reveals the top card of their library. Put that card into your hand and you lose life equal to its mana value."},
            {"67f", "roll a six-sided die. That player loses life equal to the result."},
        };
        for (String[] t : texts) {
            Card spy = printing("Sly Spy", t[0], me, ZoneType.Battlefield);
            AssertJUnit.assertEquals(t[0], "Whenever this creature deals combat damage to a player, " + t[1], spy.getAbilityText().trim());
        }
    }

    @Test
    public void testFacing() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        // Scryfall Tagger: 67d faces right, the other five left; Hangman's art isn't tagged either way
        AssertJUnit.assertEquals("left", printing("Sly Spy", "67b", me, ZoneType.Battlefield).getArtFacing());
        AssertJUnit.assertEquals("right", printing("Sly Spy", "67d", me, ZoneType.Battlefield).getArtFacing());
        AssertJUnit.assertNull(printing("Hangman", "56", me, ZoneType.Battlefield).getArtFacing());
    }

    @Test
    public void testLongestName() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (String name : new String[] {"Hill Giant", "Grizzly Bears", "Craw Wurm"}) {
            addCardToZone(name, opp, ZoneType.Hand);
        }
        attack(game, ai, "67a");
        AssertJUnit.assertEquals(18, opp.getLife());
        // "Grizzly Bears" is 12 characters without its space
        AssertJUnit.assertEquals("Grizzly Bears", opp.getCardsIn(ZoneType.Graveyard).getFirst().getName());
        AssertJUnit.assertEquals(2, opp.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testDestroyFacingLeft() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // tapped, so they can't block: one known to face left, one unknown, one known to face right
        Card left = printing("Sly Spy", "67a", opp, ZoneType.Battlefield);
        Card unknown = printing("Hangman", "56", opp, ZoneType.Battlefield);
        Card right = printing("Sly Spy", "67d", opp, ZoneType.Battlefield);
        for (Card c : new Card[] {left, unknown, right}) {
            c.setTapped(true);
        }
        attack(game, ai, "67b");
        AssertJUnit.assertTrue(game.getCardState(left).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(unknown).isInPlay());
        AssertJUnit.assertTrue(game.getCardState(right).isInPlay());
    }

    @Test
    public void testLoseAFingerWhileItsAround() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card spy = attack(game, ai, "67c");
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Command).size());
        AssertJUnit.assertEquals("Lost Finger", opp.getCardsIn(ZoneType.Command).getFirst().getName());
        game.getAction().destroy(spy, null, false, forge.game.ability.AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Command).size());
    }

    @Test
    public void testTakeTheTopCard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCardToZone("Hill Giant", opp, ZoneType.Library);
        attack(game, ai, "67e");
        // in my hand, still the opponent's card, and I lose 4
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Hand).contains(game.getCardState(giant)));
        AssertJUnit.assertEquals(opp, game.getCardState(giant).getOwner());
        AssertJUnit.assertEquals(16, ai.getLife());
        AssertJUnit.assertEquals(18, opp.getLife());
    }

    @Test
    public void testRollForLifeLoss() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        attack(game, ai, "67f");
        AssertJUnit.assertTrue("life " + opp.getLife(), opp.getLife() >= 12 && opp.getLife() <= 17);
    }
}
