package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Black Lotus Unknown Planechase (PUNK), third part: the booster phenomena, Year-End Review and the Artist Alley plane,
 * checked against Scryfall 2026-10-22 (no rulings). User rulings 2026-10-22: each player picks their pack's set (the AI
 * at random); Unknown Event's cards are each player's own; The Pro Tour's draft is one card from your own pack; a plane
 * may share its name with another card.
 */
public class BlackLotusUnknownPlanechase3Test extends AITest {

    private static Card planeswalk(final Game game, final Player p, final String name) {
        final PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name);
        AssertJUnit.assertNotNull("no plane card " + name, pc);
        final Card plane = Card.fromPaperCard(pc, p);
        if (p.getZone(ZoneType.PlanarDeck).isEmpty()) {
            p.getZone(ZoneType.PlanarDeck).add(Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard("Bicycle Rack"), p));
        }
        p.getZone(ZoneType.PlanarDeck).add(plane, 0);
        p.leaveCurrentPlane();
        p.planeswalkTo(null, new CardCollection(plane));
        resolveAll(game);
        return game.getCardState(plane);
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
        game.getAction().checkStateEffects(true);
    }

    private Game start() {
        final Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void testArtistAlleySharesItsName() {
        initAndCreateGame();
        final PaperCard land = FModel.getMagicDb().getCommonCards().getCard("Artist Alley");
        final PaperCard plane = FModel.getMagicDb().getVariantCards().getCard("Artist Alley");
        AssertJUnit.assertNotNull(land);
        AssertJUnit.assertNotNull(plane);
        AssertJUnit.assertTrue(land.getRules().getType().isLand());
        AssertJUnit.assertTrue(plane.getRules().getType().isPlane());
        AssertJUnit.assertEquals("UNK", land.getEdition());
        AssertJUnit.assertEquals("PUNK", plane.getEdition());
        AssertJUnit.assertTrue("no plane printing in Unknown Event", FModel.getMagicDb().getVariantCards().getAllCards("Artist Alley")
                .stream().allMatch(pc -> pc.getEdition().equals("PUNK")));
        AssertJUnit.assertTrue("no land printing in the Planechase set", FModel.getMagicDb().getCommonCards().getAllCards("Artist Alley")
                .stream().allMatch(pc -> pc.getEdition().equals("UNK")));
    }

    @Test
    public void testArtistAlleyPlane() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        final int exiled = ai.getCardsIn(ZoneType.Exile).size();
        planeswalk(game, ai, "Artist Alley");
        AssertJUnit.assertEquals(exiled + 1, ai.getCardsIn(ZoneType.Exile).size());
        final Card top = ai.getCardsIn(ZoneType.Exile).get(ai.getCardsIn(ZoneType.Exile).size() - 1);
        AssertJUnit.assertFalse("it may be played this turn", top.mayPlay(ai).isEmpty());

        PlanarDice.roll(ai, PlanarDice.Chaos);
        resolveAll(game);
        AssertJUnit.assertTrue("six exiled, and a nonland one cast for free",
                CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Runeclaw Bear")) == 1);
    }

    @Test
    public void testUnknownEvent() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card event = planeswalk(game, ai, "Unknown Event");
        AssertJUnit.assertFalse(event.isInZone(ZoneType.Command));
        for (final Player p : game.getPlayers()) {
            final CardCollection mine = CardLists.filter(p.getCardsIn(ZoneType.Exile), Card::isFaceDown);
            AssertJUnit.assertTrue("up to two, face down", mine.size() <= 2);
            for (final Card c : mine) {
                AssertJUnit.assertTrue(c.mayPlayerLook(p));
                AssertJUnit.assertFalse("only their own", c.mayPlayerLook(p.equals(ai) ? human : ai));
                AssertJUnit.assertFalse(c.mayPlay(p).isEmpty());
                AssertJUnit.assertEquals("free", forge.game.card.CardPlayOption.PayManaCost.NO, c.mayPlay(p).get(0).getPayManaCost());
            }
        }
    }

    @Test
    public void testTheProTour() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        planeswalk(game, ai, "The Pro Tour");
        for (final Player p : game.getPlayers()) {
            final CardCollection drafted = CardLists.filter(p.getCardsIn(ZoneType.Exile), Card::isFaceDown);
            AssertJUnit.assertEquals("one drafted card each", 1, drafted.size());
            AssertJUnit.assertFalse(drafted.get(0).mayPlay(p).isEmpty());
            AssertJUnit.assertTrue("any type of mana", drafted.get(0).mayPlay(p).get(0).isIgnoreManaCostType());
        }

        fillLibrary(ai, 10);
        fillLibrary(human, 10);
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCard("Llanowar Elves", ai);
        final Card old = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Black Lotus"), ai);
        ai.getZone(ZoneType.Battlefield).add(old);
        AssertJUnit.assertFalse(old.isValid("Card.StandardLegal", ai, old, null));
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final int expected = (int) java.util.stream.Stream.of("Forest", "Llanowar Elves").filter(n -> forge.game.GameFormat.currentStandard()
                .getLegalFrontFaceNames().contains(n)).count();
        PlanarDice.roll(ai, PlanarDice.Chaos);
        resolveAll(game);
        AssertJUnit.assertEquals("differently named Standard-legal cards", hand + expected, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testRoyalBooster() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card booster = planeswalk(game, ai, "Royal Booster");
        AssertJUnit.assertFalse(booster.isInZone(ZoneType.Command));
        AssertJUnit.assertTrue(ai.isMonarch());
        final Card deck = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().equals("Royal Booster's Booster Deck")).get(0);
        final int size = deck.getPackUnder().size();
        AssertJUnit.assertTrue(size >= 10);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final int drawn = ai.getNumDrawnThisTurn();
        final SpellAbility draw = AbilityFactory.getAbility("DB$ OpenBooster | DrawFromUnder$ True | Defined$ You", deck);
        draw.setActivatingPlayer(ai);
        AbilityUtils.resolve(draw);
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("it's a draw", drawn + 1, ai.getNumDrawnThisTurn());
        AssertJUnit.assertEquals(size - 1, deck.getPackUnder().size());
        AssertJUnit.assertTrue(deck.isRemembered(ai.getCardsIn(ZoneType.Hand).get(ai.getCardsIn(ZoneType.Hand).size() - 1)));
        AssertJUnit.assertEquals(1, deck.getTriggers().stream().filter(t -> t.getMode() == forge.game.trigger.TriggerType.Phase).count());
        AssertJUnit.assertFalse(human.isMonarch());
    }

    @Test
    public void testYearEndReview() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card giant = addCard("Hill Giant", ai);
        final Card review = planeswalk(game, ai, "Year-End Review");
        AssertJUnit.assertFalse(review.isInZone(ZoneType.Command));
        final Card now = game.getCardState(giant);
        AssertJUnit.assertTrue("promoted to commander", now.isCommander());
        AssertJUnit.assertTrue(ai.getCommanders().contains(now));
    }
}
