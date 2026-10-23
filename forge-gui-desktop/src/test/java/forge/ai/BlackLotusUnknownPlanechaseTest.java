package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Black Lotus Unknown Planechase (PUNK) planes and phenomena, checked against Scryfall 2026-10-22 (none has rulings).
 * Phenomena without "planeswalk away" text still go away once their trigger is done (CR 704.6f).
 */
public class BlackLotusUnknownPlanechaseTest extends AITest {

    private static Card planeswalk(final Game game, final Player p, final String name) {
        final PaperCard pc = FModel.getMagicDb().getVariantCards().getCard(name);
        AssertJUnit.assertNotNull("no plane card " + name, pc);
        final Card plane = Card.fromPaperCard(pc, p);
        // a phenomenon planeswalks away at once, so the deck needs a plane to go to
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

    private static void chaos(final Game game, final Player p) {
        PlanarDice.roll(p, PlanarDice.Chaos);
        resolveAll(game);
    }

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private Game start() {
        final Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    private static int count(final Player p, final String name) {
        return CardLists.count(p.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals(name));
    }

    @Test
    public void testHighAndDryBlackMarket() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(human, 10);
        fillLibrary(ai, 10);
        final int before = human.getCardsIn(ZoneType.Hand).size() + human.getCardsIn(ZoneType.Battlefield).size()
                + ai.getCardsIn(ZoneType.Hand).size() + ai.getCardsIn(ZoneType.Battlefield).size();
        final Card market = planeswalk(game, ai, "High and Dry Black Market");
        AssertJUnit.assertFalse("planeswalked away", market.isInZone(ZoneType.Command));
        final int after = human.getCardsIn(ZoneType.Hand).size() + human.getCardsIn(ZoneType.Battlefield).size()
                + ai.getCardsIn(ZoneType.Hand).size() + ai.getCardsIn(ZoneType.Battlefield).size();
        AssertJUnit.assertTrue("both players got their modes", after >= before + 4);
    }

    @Test
    public void testTeamUpNeighbors() {
        final Game game = initAndCreateThreePlayerGame();
        final Player a = game.getPlayers().get(0);
        final Player b = game.getPlayers().get(1);
        final Player c = game.getPlayers().get(2);
        final Card host = addCard("Grizzly Bears", a);
        AssertJUnit.assertFalse(a.isValid("Player.NeighborsEliminated", a, host, null));
        b.loseConditionMet(forge.game.player.GameLossReason.Conceded, null);
        AssertJUnit.assertFalse("one neighbor isn't both", a.isValid("Player.NeighborsEliminated", a, host, null));
        c.loseConditionMet(forge.game.player.GameLossReason.Conceded, null);
        AssertJUnit.assertTrue(a.isValid("Player.NeighborsEliminated", a, host, null));
    }

    @Test
    public void testTeamUpLeaves() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card team = planeswalk(game, ai, "Team-Up!");
        AssertJUnit.assertFalse(team.isInZone(ZoneType.Command));
        AssertJUnit.assertEquals(1, CardLists.count(ai.getCardsIn(ZoneType.Command), x -> x.getName().equals("Team-Up! Effect")));
    }

    @Test
    public void testProbabilityFlux() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card flux = planeswalk(game, ai, "Probability Flux");
        AssertJUnit.assertFalse(flux.isInZone(ZoneType.Command));
        final int rolls = ai.getNumRollsThisTurn();
        final Card host = addCard("Grizzly Bears", ai);
        final SpellAbility roll = AbilityFactory.getAbility("DB$ RollDice | Sides$ 6", host);
        roll.setActivatingPlayer(ai);
        AbilityUtils.resolve(roll);
        AssertJUnit.assertEquals("one die plus one", rolls + 2, ai.getNumRollsThisTurn());
    }

    @Test
    public void testClamiltonEstate() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        planeswalk(game, ai, "Clamilton Estate");
        final Card drifter = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Mulldrifter", "LRW"), ai);
        AssertJUnit.assertTrue(drifter.isWordy());
        ai.getZone(ZoneType.Hand).add(drifter);
        final int hand = ai.getCardsIn(ZoneType.Hand).size() - 1;
        game.getAction().moveToPlay(drifter, ai, null, AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals("its enters trigger twice", hand + 4, ai.getCardsIn(ZoneType.Hand).size());

        final Card wordy = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Mulldrifter", "LRW"), ai);
        ai.getZone(ZoneType.Library).add(wordy, 3);
        final int library = ai.getCardsIn(ZoneType.Library).size();
        chaos(game, ai);
        AssertJUnit.assertTrue(game.getCardState(wordy).isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals("the rest go to the bottom", library - 1, ai.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testEventHorizon() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card tower = addCard("Command Tower", ai);
        final Card forest = addCard("Forest", ai);
        planeswalk(game, ai, "Event Horizon");
        AssertJUnit.assertEquals("Wastes", tower.getName());
        AssertJUnit.assertTrue(tower.getType().isBasic());
        AssertJUnit.assertTrue(tower.getType().isLand());
        AssertJUnit.assertEquals(1, tower.getManaAbilities().size());
        AssertJUnit.assertEquals("Forest", forest.getName());
        final SpellAbility mana = tower.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        ai.getController().playChosenSpellAbility(mana);
        resolveAll(game);
        AssertJUnit.assertEquals("an additional {C}", 2, ai.getManaPool().totalMana());
    }

    @Test
    public void testTheSphere() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card sphere = planeswalk(game, ai, "The Sphere");
        final Card bear = addCard("Grizzly Bears", ai);
        final SpellAbility round = svar(sphere, "TrigRound", ai);
        round.setTriggeringObject(AbilityKey.Attacker, bear);
        AbilityUtils.resolve(round);
        AssertJUnit.assertEquals(5, bear.getNetPower());
        AssertJUnit.assertEquals(5, bear.getNetToughness());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("it stays rounded", 5, bear.getNetPower());

        chaos(game, ai);
        final Card army = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Army")).get(0);
        AssertJUnit.assertTrue(army.getType().hasCreatureType("Fan"));
        AssertJUnit.assertTrue(army.getColor().hasRed());
        AssertJUnit.assertEquals(5, army.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testGameKnightsLive() {
        final Game game = start();
        final Player ai = game.getPlayers().get(1);
        final Card plane = planeswalk(game, ai, "Game Knights Live");
        final Card commander = addCard("Grizzly Bears", ai);
        ai.addCommander(commander);
        final SpellAbility knight = svar(plane, "TrigKnightAttack", ai);
        knight.setTriggeringObject(AbilityKey.Attacker, commander);
        AbilityUtils.resolve(knight);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(commander.getType().hasCreatureType("Knight"));
        AssertJUnit.assertTrue(commander.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertTrue(commander.hasKeyword(Keyword.TRAMPLE));

        chaos(game, ai);
        AssertJUnit.assertEquals("it gains the draw trigger", 1, commander.getTriggers().size());

        planeswalk(game, ai, "The Sphere");
        AssertJUnit.assertFalse("not a Knight once a player planeswalks away", commander.getType().hasCreatureType("Knight"));
    }

    @Test
    public void testTheCommandZone() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(human, 5);
        final Card mine = addCard("Grizzly Bears", ai);
        ai.addCommander(mine);
        final Card theirs = addCardToZone("Hill Giant", human, ZoneType.Command);
        human.addCommander(theirs);
        human.incCommanderCast(theirs);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final int theirHand = human.getCardsIn(ZoneType.Hand).size();
        planeswalk(game, ai, "The Command Zone");
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("no commander out, no card", theirHand, human.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue("the AI puts its commander out", game.getCardState(theirs).isInZone(ZoneType.Battlefield));

        AssertJUnit.assertEquals(1, human.getCommanderCast(game.getCardState(theirs)));
        chaos(game, ai);
        AssertJUnit.assertEquals(0, human.getCommanderCast(game.getCardState(theirs)));
        AssertJUnit.assertEquals(0, human.getCommanderTax(game.getCardState(theirs)));
    }

    @Test
    public void testHighNoon() {
        final Game game = start();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(human, 5);
        planeswalk(game, ai, "High Noon At Thunder Junction");
        // the drawer draws two lands and reveals a third, the opponent reveals an 11-drop
        for (int i = 0; i < 3; i++) {
            ai.getZone(ZoneType.Library).add(createCard("Forest", ai), 0);
        }
        human.getZone(ZoneType.Library).add(createCard("Darksteel Colossus", human), 0);
        ai.drawCards(2);
        resolveAll(game);
        AssertJUnit.assertEquals("the opponent won the clash", 2, count(human, "Assassin Token"));
        AssertJUnit.assertEquals(0, count(ai, "Assassin Token"));
    }
}
