package forge.ai;

import forge.card.ICardFace;
import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Secret Lair Showcase Planes (PSSC), checked against Scryfall 2026-10-19 (no rulings on any of the ten).
 */
public class SecretLairShowcasePlanesTest extends AITest {

    private static Card planeswalk(Game game, Player p, String name) {
        PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name);
        AssertJUnit.assertNotNull("no plane card " + name, pc);
        Card plane = Card.fromPaperCard(pc, p);
        // a real game planeswalks to the top card of the planar deck
        p.getZone(ZoneType.PlanarDeck).add(plane);
        p.planeswalkTo(null, new CardCollection(plane));
        resolveAll(game);
        return game.getCardState(plane);
    }

    private static void resolveAll(Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
        game.getAction().checkStateEffects(true);
    }

    private static void chaos(Game game, Player p) {
        PlanarDice.roll(p, PlanarDice.Chaos);
        resolveAll(game);
    }

    private Game start() {
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void allTenPlanesLoad() {
        for (String name : new String[] {"Deceptive Divination", "Fblthp: Completely, Utterly, Totally Lost",
                "Finally! Left-Handed Magic Cards", "Happy Yargle Day!", "Imaginary Friends (Plane)",
                "Li'l Giri Saves the Day", "Math is for Blockers (Plane)", "Mycosynthwave", "sAnS mERcY",
                "We Hope You Like Squirrels"}) {
            PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name, "PSSC");
            AssertJUnit.assertNotNull("PSSC print of " + name, pc);
            AssertJUnit.assertTrue(name, pc.getRules().getType().isPlane());
        }
    }

    @Test
    public void deceptiveDivinationMakesSorceriesInstants() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card retrieval = addCardToZone("Mystic Retrieval", ai, ZoneType.Hand);
        Card divination = addCardToZone("Divination", ai, ZoneType.Graveyard);
        Card plane = planeswalk(game, ai, "Deceptive Divination");
        AssertJUnit.assertTrue(retrieval.isInstant());
        AssertJUnit.assertFalse(retrieval.isSorcery());
        AssertJUnit.assertTrue("not on the stack, still changed", divination.isInstant());
        // its own text: "instant or sorcery card" now reads "instant or instant card"
        String tgts = retrieval.getFirstSpellAbility().getParam("ValidTgts");
        AssertJUnit.assertFalse(tgts, tgts.contains("Sorcery"));

        ai.leaveCurrentPlane();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(game.getActivePlanes().contains(plane));
        AssertJUnit.assertTrue(retrieval.isSorcery());
        AssertJUnit.assertTrue(retrieval.getFirstSpellAbility().getParam("ValidTgts").contains("Sorcery"));
    }

    @Test
    public void finallySwapsPrintedPowerAndToughness() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card wurm = addCard("Craw Wurm", ai);
        Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        planeswalk(game, ai, "Finally! Left-Handed Magic Cards");
        AssertJUnit.assertEquals(4, wurm.getNetPower());
        AssertJUnit.assertEquals(6, wurm.getNetToughness());
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals(3, bears.getNetToughness());

        Card bottom = addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        Card top = ai.getCardsIn(ZoneType.Library).getFirst();
        Card last = ai.getCardsIn(ZoneType.Library).getLast();
        chaos(game, ai);
        AssertJUnit.assertTrue("drew the bottom card", game.getCardState(last).isInZone(ZoneType.Hand));
        AssertJUnit.assertTrue(game.getCardState(top).isInZone(ZoneType.Library));
        AssertJUnit.assertNotNull(bottom);
    }

    @Test
    public void lilGiriCountsPlanarDieRolls() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        planeswalk(game, ai, "Li'l Giri Saves the Day");
        Card giri = findCardWithName(game, "Li'l Giri");
        AssertJUnit.assertNotNull(giri);
        AssertJUnit.assertTrue(giri.isToken() && giri.getType().isLegendary() && giri.getType().hasSubtype("Food"));
        AssertJUnit.assertTrue(giri.hasKeyword(Keyword.HEXPROOF));
        AssertJUnit.assertTrue(giri.hasKeyword(Keyword.INDESTRUCTIBLE));
        chaos(game, ai);
        AssertJUnit.assertEquals(1, giri.getCounters(CounterEnumType.P1P1));
        chaos(game, ai);
        AssertJUnit.assertEquals("second roll this turn puts two more", 3, giri.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, ai.getNumPlanarDieRollsThisTurn());
    }

    @Test
    public void sansMercyDestroysAndRepeatsPerRoll() {
        Game game = start();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", opp);
        Card zombie = addCard("Walking Corpse", opp);
        Card elf = addCard("Llanowar Elves", ai);
        planeswalk(game, ai, "sAnS mERcY");
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("black isn't chosen", zombie.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(elf).isInZone(ZoneType.Graveyard));

        // nothing to sacrifice but the Zombie, no cards to discard
        game.getAction().destroy(zombie, null, false, null);
        game.getAction().checkStateEffects(true);
        opp.getZone(ZoneType.Hand).removeAllCards(true);
        int life = opp.getLife();
        chaos(game, ai);
        AssertJUnit.assertEquals(life - 3, opp.getLife());
        chaos(game, ai);
        AssertJUnit.assertEquals("twice on the second roll", life - 9, opp.getLife());
    }

    @Test
    public void squirrelsGetCountersOnChaos() {
        Game game = start();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        planeswalk(game, ai, "We Hope You Like Squirrels");
        List<Card> mine = addTokens("g_1_1_squirrel", 2, ai);
        Card theirs = addToken("g_1_1_squirrel", opp);
        chaos(game, ai);
        for (Card c : mine) {
            AssertJUnit.assertEquals(1, c.getCounters(CounterEnumType.P1P1));
        }
        AssertJUnit.assertEquals(0, theirs.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void squirrelEachUpkeep() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        planeswalk(game, ai, "We Hope You Like Squirrels");
        playUntilPhase(game, PhaseType.UPKEEP);
        playUntilStackClear(game);
        Player active = game.getPhaseHandler().getPlayerTurn();
        AssertJUnit.assertEquals(1, active.getCreaturesInPlay().filter(c -> c.getType().hasCreatureType("Squirrel")).size());
    }

    @Test
    public void fblthpMapsAndLoots() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Island", ai, ZoneType.Hand);
        List<Card> lands = addCards("Forest", 4, ai);
        planeswalk(game, ai, "Fblthp: Completely, Utterly, Totally Lost");
        AssertJUnit.assertNotNull(findCardWithName(game, "Map Token"));
        for (Card l : lands) {
            l.tap(true, null, null);
        }
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        chaos(game, ai);
        AssertJUnit.assertEquals(hand, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(3, lands.stream().filter(l -> !l.isTapped()).count());
    }

    @Test
    public void happyYargleDayAtEndStep() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        planeswalk(game, ai, "Happy Yargle Day!");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        playUntilPhase(game, PhaseType.END_OF_TURN);
        playUntilStackClear(game);
        bears = game.getCardState(bears);
        AssertJUnit.assertEquals("Yargle, Glutton of Urborg", bears.getName());
        AssertJUnit.assertFalse(bears.getType().isLegendary());
        AssertJUnit.assertEquals(9, bears.getNetPower());
        AssertJUnit.assertEquals(1, bears.getCounters(CounterType.getType("BARGLE")));
    }

    @Test
    public void imaginaryFriendsChoices() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        planeswalk(game, ai, "Imaginary Friends (Plane)");
        List<ICardFace> choices = StaticAbilityCastAs.anotherCardChoices(bolt);
        AssertJUnit.assertTrue(choices.stream().anyMatch(f -> f.getName().equals("Burst Lightning")));
        AssertJUnit.assertTrue(choices.stream().noneMatch(f -> f.getName().equals("Lightning Bolt")));
        AssertJUnit.assertTrue("same type", choices.stream().noneMatch(f -> f.getName().equals("Firebolt")));
        // the AI isn't offered the stand-in, it can't weigh a card it isn't holding
        AssertJUnit.assertTrue(bolt.getAllPossibleAbilities(ai, true).stream().noneMatch(SpellAbility::isCastAsAnother));

        SpellAbility as = StaticAbilityCastAs.chooseAnotherCard(bolt, ai, bolt.getFirstSpellAbility());
        AssertJUnit.assertNotNull(as);
        AssertJUnit.assertNotNull(as.getCastAs());
        AssertJUnit.assertFalse("Lightning Bolt".equals(as.getCastAs()));
    }

    @Test
    public void mathIsForBlockersReturnsNextDeclareAttackers() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        fillLibrary(game.getPlayers().get(0), 10);
        Card giant = addCard("Hill Giant", ai);
        giant.setSickness(false);
        planeswalk(game, ai, "Math is for Blockers (Plane)");
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        playUntilStackClear(game);
        AssertJUnit.assertTrue("exiled on attack", game.getCardState(giant).isInZone(ZoneType.Exile));

        playUntilNextTurn(game);
        AssertJUnit.assertTrue("not back on the other player's turn", game.getCardState(giant).isInZone(ZoneType.Exile));
        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS);
        playUntilStackClear(game);
        Card back = game.getCardState(giant);
        AssertJUnit.assertTrue("back: " + back.getZone(), back.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(back.isTapped());
        AssertJUnit.assertTrue(game.getCombat() != null && game.getCombat().isAttacking(back));
    }
}
