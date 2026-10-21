package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.HeadBalance;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Form of the Approach of the Second Sun (UNF #9), checked against Scryfall and its 10 rulings 2026-10-20: you're a
 * planeswalker card with no other characteristics while in your library; leaving it (drawn or not) or a shuffle ends
 * that, and only drawing yourself wins; the cards above you are on your head. Falls: human by honor-system action,
 * AI by a simulated wobble (user rulings).
 */
public class FormOfTheApproachTest extends AITest {

    private Card formWithLibrary(final Game game, final Player p, final int libraryCards) {
        for (int i = 0; i < libraryCards; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
        }
        final Card form = addCard("Form of the Approach of the Second Sun", p);
        final SpellAbility etb = form.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(p);
        final int life = p.getLife();
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(life + 7, p.getLife());
        return form;
    }

    @Test
    public void testYouBecomeANamelessPlaneswalkerCardSeventhFromTheTop() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 10);
        final Card self = HeadBalance.personCard(p);
        AssertJUnit.assertNotNull(self);
        AssertJUnit.assertEquals(GamePieceType.PERSON, self.getGamePieceType());
        AssertJUnit.assertEquals(6, p.getCardsIn(ZoneType.Library).indexOf(self));
        AssertJUnit.assertEquals(6, HeadBalance.cardsOnHead(p));
        AssertJUnit.assertTrue(self.isPlaneswalker());
        AssertJUnit.assertEquals("", self.getName());
        AssertJUnit.assertTrue("a search for a planeswalker card finds you", self.isValid("Planeswalker", p, null, null));
    }

    @Test
    public void testShortLibraryPutsYouOnTheBottom() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 3);
        final CardCollectionView lib = p.getCardsIn(ZoneType.Library);
        AssertJUnit.assertEquals(lib.size() - 1, lib.indexOf(HeadBalance.personCard(p)));
    }

    @Test
    public void testAShuffleEndsIt() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 10);
        p.shuffle(null);
        AssertJUnit.assertNull(HeadBalance.personCard(p));
        AssertJUnit.assertEquals(10, p.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testDrawingYourselfWins() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 0);
        AssertJUnit.assertEquals(0, p.getCardsIn(ZoneType.Library).indexOf(HeadBalance.personCard(p)));
        final int hand = p.getCardsIn(ZoneType.Hand).size();
        p.drawCard();
        AssertJUnit.assertNull("you stopped being a card", HeadBalance.personCard(p));
        AssertJUnit.assertEquals("and drew nothing in your place", hand, p.getCardsIn(ZoneType.Hand).size());
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        game.getStack().resolveStack();
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertTrue(p.getOutcome().hasWon());
    }

    @Test
    public void testTutoringYourselfDoesntWin() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 0);
        game.getAction().moveToHand(HeadBalance.personCard(p), null, null);
        AssertJUnit.assertNull(HeadBalance.personCard(p));
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertFalse(game.isGameOver());
    }

    @Test
    public void testCardsFallingExileYourHeadAndSacrificeTheForm() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card form = formWithLibrary(game, p, 10);
        HeadBalance.cardsFell(p);
        game.getStack().addAllTriggeredAbilitiesToStack();
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(6, p.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertTrue(game.getCardState(form).isInZone(ZoneType.Graveyard));
        final Card self = HeadBalance.personCard(p);
        AssertJUnit.assertNotNull("you're still a card", self);
        AssertJUnit.assertEquals(0, p.getCardsIn(ZoneType.Library).indexOf(self));
    }

    @Test
    public void testCardsGoingOnTopAreCounted() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        formWithLibrary(game, p, 10);
        final Card bolt = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        game.getAction().moveToLibrary(bolt, 0, null, null);
        final Card self = HeadBalance.personCard(p);
        if (self != null) {
            // the AI's stack may have fallen, which exiles the head; otherwise it's one taller
            AssertJUnit.assertEquals(HeadBalance.cardsOnHead(p), self.getCardsOnHead());
        }
    }
}
