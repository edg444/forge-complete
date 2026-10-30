package forge.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.herospath.HerosPath;
import forge.game.herospath.HerosPath.Challenge;
import forge.game.herospath.HerosPath.Difficulty;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * The Hero's Path challenges against their playmats (Battle the Horde: Wizards' PDF; Face the Hydra and Defeat a God:
 * mtg.wiki's transcriptions), the Hero's Path FAQ, and the user's rulings of 2026-10-30.
 */
public class HerosPathTest extends AITest {

    /** Heroes (AI) in seats 0..n-1, the challenge deck last; its library in decklist order, top card first. */
    private Game createChallengeGame(final Challenge c, final Difficulty d, final List<List<String>> heroCards) {
        initAndCreateGame();
        final List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < heroCards.size(); i++) {
            final RegisteredPlayer rp = new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("hero" + i, null));
            HerosPath.prepareHero(rp, heroCards.get(i));
            players.add(rp);
        }
        players.add(HerosPath.registerChallenge(c, d, heroCards.size()).setPlayer(new LobbyPlayerChallenge(c.opponentName)));
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(c.gameType));
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        for (int i = 0; i < players.size(); i++) {
            final Player p = game.getPlayers().get(i);
            p.initVariantsZones(players.get(i));
            if (HerosPath.isChallengeDeck(p)) {
                for (final PaperCard pc : players.get(i).getDeck().getMain().toFlatList()) {
                    p.getZone(ZoneType.Library).add(Card.fromPaperCard(pc, p));
                }
            } else {
                fillLibrary(p, 20);
            }
        }
        // as AITest.addCard does: the game's start would give them timestamps
        for (final Card onBattlefield : game.getCardsIn(ZoneType.Battlefield)) {
            onBattlefield.setGameTimestamp(game.getNextTimestamp());
        }
        HerosPath.onGameStarted(game);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        game.getAction().checkStateEffects(true);
        return game;
    }

    private Game createChallengeGame(final Challenge c, final int heroes) {
        final List<List<String>> none = new ArrayList<>();
        for (int i = 0; i < heroes; i++) {
            none.add(List.of());
        }
        return createChallengeGame(c, Difficulty.NORMAL, none);
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static Player challenger(final Game game) {
        return HerosPath.getChallengePlayer(game);
    }

    /** Puts the named cards of the challenge deck's library on top, in this order. */
    private static void stackLibrary(final Player opp, final String... names) {
        for (int i = names.length - 1; i >= 0; i--) {
            final String name = names[i];
            final Card c = CardLists.filter(opp.getCardsIn(ZoneType.Library), x -> x.getName().equals(name)).get(0);
            opp.getZone(ZoneType.Library).remove(c);
            opp.getZone(ZoneType.Library).add(c, 0);
        }
    }

    private static CardCollection named(final Game game, final String name) {
        return CardLists.filter(game.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals(name));
    }

    private Card cast(final Game game, final Player p, final String name, final GameObject target) {
        final Card inHand = addCardToZone(name, p, ZoneType.Hand);
        final Card onStack = game.getAction().moveToStack(inHand, null);
        final SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        game.getStack().add(sa);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        return onStack;
    }

    @Test
    public void allChallengeCardsLoad() {
        initAndCreateGame();
        for (final Challenge c : Challenge.values()) {
            int total = 0;
            for (final var e : c.deckList().entrySet()) {
                AssertJUnit.assertNotNull(e.getKey(), StaticData.instance().getCommonCards().getCard(e.getKey()));
                total += e.getValue();
            }
            AssertJUnit.assertEquals(c.getName(), 60, total);
        }
        // 21 Hero cards (Scryfall thp1, thp2, thp3), and the seven Enhancement Stickers
        AssertJUnit.assertEquals(21, HerosPath.allHeroCards().size());
        AssertJUnit.assertEquals(7, HerosPath.enhancementDescriptions().size());
    }

    @Test
    public void hydraSetUp() {
        final Game game = createChallengeGame(Challenge.HYDRA, 1);
        final Player hero = game.getPlayers().get(0), hydra = challenger(game);

        AssertJUnit.assertEquals(3, named(game, "Hydra Head").size()); // Normal: 3 starting Heads
        AssertJUnit.assertEquals(57, hydra.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(hydra.getCardsIn(ZoneType.Hand).isEmpty());
        AssertJUnit.assertEquals(1, hydra.getCardsIn(ZoneType.Command).size());
        AssertJUnit.assertEquals(hero, HerosPath.firstPlayer(game)); // "You go first."
        AssertJUnit.assertEquals(20, hero.getLife());

        // Heads can be attacked directly, the Hydra too
        final Card bear = addCard("Grizzly Bears", hero);
        final var defenders = CombatUtil.getAllPossibleDefenders(hero);
        for (final Card head : named(game, "Hydra Head")) {
            AssertJUnit.assertTrue(defenders.contains(head));
        }
        AssertJUnit.assertTrue(defenders.contains(hydra));
        AssertJUnit.assertNotNull(bear);

        // Hard: 4 Heads; Easy: 2
        AssertJUnit.assertEquals(4, named(createChallengeGame(Challenge.HYDRA, Difficulty.HARD, List.of(List.of())), "Hydra Head").size());
        AssertJUnit.assertEquals(2, named(createChallengeGame(Challenge.HYDRA, Difficulty.EASY, List.of(List.of())), "Hydra Head").size());
    }

    @Test
    public void hydraGrowsNewHeadsAndRewards() {
        final Game game = createChallengeGame(Challenge.HYDRA, 1);
        final Player hero = game.getPlayers().get(0), hydra = challenger(game);
        stackLibrary(hydra, "Ravenous Brute Head", "Unified Lunge");

        cast(game, hero, "Murder", named(game, "Hydra Head").get(0));
        resolveAll(game);

        // Hero's Reward, then the top two revealed: the Head onto the battlefield, the sorcery into the graveyard
        AssertJUnit.assertEquals(22, hero.getLife());
        AssertJUnit.assertEquals(2, named(game, "Hydra Head").size());
        AssertJUnit.assertEquals(1, named(game, "Ravenous Brute Head").size());
        AssertJUnit.assertEquals(1, CardLists.filter(hydra.getCardsIn(ZoneType.Graveyard), c -> c.getName().equals("Unified Lunge")).size());
        AssertJUnit.assertEquals(2, hydra.getCardsIn(ZoneType.Graveyard).size()); // and the Hydra Head
    }

    @Test
    public void damageToTheHydraHitsAHead() {
        final Game game = createChallengeGame(Challenge.HYDRA, 1);
        final Player hero = game.getPlayers().get(0), hydra = challenger(game);
        stackLibrary(hydra, "Unified Lunge", "Unified Lunge");

        cast(game, hero, "Lightning Bolt", hydra);
        resolveAll(game);

        AssertJUnit.assertEquals(20, hydra.getLife());
        // a Hydra Head took the 3 and died
        AssertJUnit.assertEquals(2, named(game, "Hydra Head").size());
        AssertJUnit.assertEquals(22, hero.getLife());

        // life loss too: that much damage to a Head
        hydra.loseLife(1, false, false, null);
        AssertJUnit.assertEquals(20, hydra.getLife());
        AssertJUnit.assertEquals(1, named(game, "Hydra Head").stream().mapToInt(Card::getDamage).sum());
    }

    @Test
    public void hydraTurnCastsAndBites() {
        final Game game = createChallengeGame(Challenge.HYDRA, 1);
        final Player hero = game.getPlayers().get(0), hydra = challenger(game);
        stackLibrary(hydra, "Ravenous Brute Head");
        named(game, "Hydra Head").get(0).setTapped(true);
        final int handBefore = hydra.getCardsIn(ZoneType.Hand).size();

        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, hydra);
        playUntilPhase(game, PhaseType.COMBAT_END);

        // starting from its upkeep, the tapped Head stays tapped: it casts the Ravenous Brute Head, then deals 1 for
        // each untapped Hydra Head (two) and 2 for each untapped elite Head (the Brute)
        AssertJUnit.assertEquals(1, named(game, "Ravenous Brute Head").size());
        AssertJUnit.assertEquals(handBefore, hydra.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(20 - 2 - 2, hero.getLife());
    }

    @Test
    public void hydraWinsAtEndOfTurnWithNoHeads() {
        final Game game = createChallengeGame(Challenge.HYDRA, 1);
        final Player hero = game.getPlayers().get(0), hydra = challenger(game);
        for (final Card c : new CardCollection(hydra.getCardsIn(ZoneType.Library))) {
            if (!c.getType().hasCreatureType("Head")) {
                continue;
            }
            hydra.getZone(ZoneType.Library).remove(c);
        }
        for (final Card head : named(game, "Hydra Head")) {
            game.getAction().destroy(head, null, true, null);
        }
        game.getAction().checkStateEffects(true);
        resolveAll(game);
        AssertJUnit.assertTrue(CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "Head").isEmpty());
        AssertJUnit.assertFalse(game.isGameOver()); // not until the end of the turn

        playUntilNextTurn(game);
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertTrue(hero.getOutcome().hasWon());
        AssertJUnit.assertTrue(hydra.hasLost());
    }

    @Test
    public void heroesTakeTurnsThenTheHydraNoFirstDraws() {
        final Game game = createChallengeGame(Challenge.HYDRA, 2);
        final Player h0 = game.getPlayers().get(0), h1 = game.getPlayers().get(1), hydra = challenger(game);
        stackLibrary(hydra, "Hydra Head");
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, h0);
        h0.incrementTurn(); // devModeSet doesn't start a turn
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(h1, game.getPhaseHandler().getPlayerTurn());
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(hydra, game.getPhaseHandler().getPlayerTurn());
        // "You go first. (Don't draw a card on your first turn.)", for each hero
        AssertJUnit.assertEquals(0, h0.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, h1.getCardsIn(ZoneType.Hand).size());
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(h0, game.getPhaseHandler().getPlayerTurn());
        // the Hydra drew nothing: one card cast from its library
        AssertJUnit.assertEquals(56, hydra.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(hydra.getCardsIn(ZoneType.Hand).isEmpty());
    }

    @Test
    public void heroCards() {
        final Game game = createChallengeGame(Challenge.HYDRA, Difficulty.NORMAL,
                List.of(List.of("The Slayer", "The Vanquisher", "Spear of the General|5")));
        final Player hero = game.getPlayers().get(0);
        // "You start the game with an additional 7 life." / "Your starting hand size is increased by one."
        AssertJUnit.assertEquals(27, hero.getLife());
        AssertJUnit.assertEquals(8, hero.getStartingHandSize());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(8, hero.getMaxHandSize());
        AssertJUnit.assertEquals(1, named(game, "The Slayer").size());
        AssertJUnit.assertTrue(named(game, "The Slayer").get(0).getType().hasType(forge.card.CardType.CoreType.HeroCard));

        // Enhancement Sticker 5: "Equipped creature has double strike."
        final Card spear = named(game, "Spear of the General").get(0);
        final Card bear = addCard("Grizzly Bears", hero);
        spear.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bear.hasDoubleStrike());
        AssertJUnit.assertEquals(4, bear.getNetPower()); // +2/+0
    }

    @Test
    public void heroCardLimits() {
        initAndCreateGame();
        AssertJUnit.assertNull(HerosPath.heroCardProblem(Challenge.HYDRA, List.of("The Slayer", "The Slayer")));
        AssertJUnit.assertNotNull(HerosPath.heroCardProblem(Challenge.HYDRA, List.of("The Slayer", "The Hunter", "The Warrior")));
        AssertJUnit.assertNotNull(HerosPath.heroCardProblem(Challenge.HORDE, List.of("The Slayer", "The Slayer")));
        AssertJUnit.assertNull(HerosPath.heroCardProblem(Challenge.GOD, List.of("The Slayer", "The Hunter", "The Warrior")));
    }

    // ---- Battle the Horde ----

    @Test
    public void hordeSetUpAndSharedLife() {
        final Game game = createChallengeGame(Challenge.HORDE, Difficulty.NORMAL, List.of(List.of("The Slayer"), List.of()));
        final Player h0 = game.getPlayers().get(0), h1 = game.getPlayers().get(1), horde = challenger(game);
        AssertJUnit.assertEquals(60, horde.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(horde.getCardsIn(ZoneType.Battlefield).isEmpty());
        // "All players facing the Minotaurs will have a shared life total of 20 life" (and The Slayer's 7)
        AssertJUnit.assertEquals(27, h0.getLife());
        h1.loseLife(5, false, false, null);
        AssertJUnit.assertEquals(22, h0.getLife());
        AssertJUnit.assertEquals(h0, HerosPath.firstPlayer(game));
    }

    @Test
    public void hordeLifeLossMillsItsLibrary() {
        final Game game = createChallengeGame(Challenge.HORDE, 1);
        final Player hero = game.getPlayers().get(0), horde = challenger(game);
        stackLibrary(horde, "Refreshing Elixir");

        cast(game, hero, "Lightning Bolt", horde);
        resolveAll(game);

        AssertJUnit.assertEquals(57, horde.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(3, horde.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(20, horde.getLife());
        // Refreshing Elixir's Hero's Reward from the graveyard: each player gains 5 life
        AssertJUnit.assertEquals(25, hero.getLife());
    }

    @Test
    public void hordeCastsTwoAndTheyAttack() {
        final Game game = createChallengeGame(Challenge.HORDE, 1);
        final Player hero = game.getPlayers().get(0), horde = challenger(game);
        stackLibrary(horde, "Minotaur Younghorn", "Phoberos Reaver");
        final Card bear = addCard("Grizzly Bears", horde);
        bear.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, horde);
        playUntilPhase(game, PhaseType.COMBAT_END);

        // both cast; haste and "attacks each turn if able" (2 + 2); the Bears don't have to, so they don't
        AssertJUnit.assertEquals(58, horde.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(16, hero.getLife());
        AssertJUnit.assertTrue(bear.isUntapped());
    }

    @Test
    public void heroesTakeThreeTurnsFirst() {
        final Game game = createChallengeGame(Challenge.HORDE, 1);
        final Player hero = game.getPlayers().get(0), horde = challenger(game);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, hero);
        hero.incrementTurn();
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(horde, game.getPhaseHandler().getPlayerTurn());
        // "You take three turns at the start of the game, before the Horde takes its first turn."
        AssertJUnit.assertEquals(3, hero.getTurn());
        AssertJUnit.assertEquals(1, horde.getTurn());
    }

    @Test
    public void hordeDefeatedWithNoLibraryAndNoCreatures() {
        final Game game = createChallengeGame(Challenge.HORDE, 1);
        final Player hero = game.getPlayers().get(0), horde = challenger(game);
        final Card goreseeker = addCard("Minotaur Goreseeker", horde);
        for (final Card c : new CardCollection(horde.getCardsIn(ZoneType.Library))) {
            horde.getZone(ZoneType.Library).remove(c);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("it still controls a creature", game.isGameOver());

        cast(game, hero, "Murder", goreseeker);
        resolveAll(game);
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertTrue(hero.getOutcome().hasWon());
    }

    // ---- Defeat a God ----

    @Test
    public void godSetUp() {
        Game game = createChallengeGame(Challenge.GOD, 1);
        AssertJUnit.assertEquals(1, named(game, "Xenagos Ascended").size());
        AssertJUnit.assertEquals(2, named(game, "Rollicking Throng").size());
        // starting on the battlefield, the Throngs revealed nothing
        AssertJUnit.assertEquals(57, challenger(game).getCardsIn(ZoneType.Library).size());

        // "For each additional player in the game, the Xenagos deck starts with one additional Rollicking Throng"
        game = createChallengeGame(Challenge.GOD, 2);
        AssertJUnit.assertEquals(3, named(game, "Rollicking Throng").size());
        game.getPlayers().get(0).loseLife(4, false, false, null);
        AssertJUnit.assertEquals(16, game.getPlayers().get(1).getLife());

        final var defenders = CombatUtil.getAllPossibleDefenders(game.getPlayers().get(0));
        AssertJUnit.assertTrue(defenders.contains(named(game, "Xenagos Ascended").get(0)));
        AssertJUnit.assertTrue(defenders.contains(named(game, "Rollicking Throng").get(0)));
    }

    @Test
    public void xenagosCantLeaveWhileRevelersRemain() {
        final Game game = createChallengeGame(Challenge.GOD, 1);
        final Player hero = game.getPlayers().get(0), xenagos = challenger(game);
        final Card god = named(game, "Xenagos Ascended").get(0);

        cast(game, hero, "Murder", god);
        resolveAll(game);
        AssertJUnit.assertTrue(god.isInPlay());

        // damage to Xenagos is dealt to Xenagos Ascended instead
        cast(game, hero, "Lightning Bolt", xenagos);
        resolveAll(game);
        AssertJUnit.assertEquals(20, xenagos.getLife());
        AssertJUnit.assertEquals(3, game.getCardState(god).getDamage());
        AssertJUnit.assertFalse(game.isGameOver());

        // no Revelers left: it can leave, and "each player wins the game"
        for (final Card throng : named(game, "Rollicking Throng")) {
            cast(game, hero, "Murder", throng);
            resolveAll(game);
        }
        cast(game, hero, "Murder", game.getCardState(god));
        resolveAll(game);
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertTrue(hero.getOutcome().hasWon());
        AssertJUnit.assertTrue(xenagos.hasLost());
    }

    @Test
    public void attackedRevelerDealsNoDamageBack() {
        final Game game = createChallengeGame(Challenge.GOD, 1);
        final Player hero = game.getPlayers().get(0);
        final Card throng = named(game, "Rollicking Throng").get(0);
        final Card bear = addCard("Grizzly Bears", hero);
        bear.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, hero);

        final Combat combat = new Combat(hero);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertTrue(CombatUtil.canAttack(bear, throng));
        combat.addAttacker(bear, throng);
        combat.setBlocked(bear, false);
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        combat.assignCombatDamage(false);
        combat.dealAssignedDamage();
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(2, throng.getDamage());
        AssertJUnit.assertEquals(0, bear.getDamage());
    }

    /** Whole games, from the match start (mulligans, the heroes' Hero cards, real turns) to someone winning. */
    @Test(timeOut = 600000)
    public void fullGamesEnd() {
        initAndCreateGame();
        final Deck heroDeck = new Deck("Green");
        heroDeck.getMain().add(StaticData.instance().getCommonCards().getCard("Forest"), 24);
        heroDeck.getMain().add(StaticData.instance().getCommonCards().getCard("Grizzly Bears"), 12);
        heroDeck.getMain().add(StaticData.instance().getCommonCards().getCard("Centaur Courser"), 12);
        heroDeck.getMain().add(StaticData.instance().getCommonCards().getCard("Giant Growth"), 12);
        for (final Challenge c : Challenge.values()) {
            final List<RegisteredPlayer> players = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final RegisteredPlayer rp = new RegisteredPlayer(heroDeck).setPlayer(new LobbyPlayerAi("hero" + i, null));
                HerosPath.prepareHero(rp, i == 0 ? List.of("The Slayer") : List.of("The Hunter"));
                players.add(rp);
            }
            players.add(HerosPath.registerChallenge(c, Difficulty.NORMAL, 2).setPlayer(new LobbyPlayerChallenge(c.opponentName)));
            final GameRules rules = new GameRules(GameType.Constructed);
            rules.setAppliedVariants(EnumSet.of(c.gameType));
            final Match match = new Match(rules, players, "Test");
            final Game game = match.createGame();
            match.startGame(game);
            AssertJUnit.assertTrue(c.getName(), game.isGameOver());
        }
    }

    @Test
    public void challengeDeckDoesNothingOfItsOwn() {
        final Game game = createChallengeGame(Challenge.GOD, 1);
        final Player xenagos = challenger(game);
        // its creatures don't attack unless a card says they do, and it never chooses to cast or activate anything
        AssertJUnit.assertNull(xenagos.getController().chooseSpellAbilityToPlay());
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, xenagos);
        for (final Card c : xenagos.getCreaturesInPlay()) {
            c.setSickness(false);
        }
        final Combat combat = new Combat(xenagos);
        xenagos.getController().declareAttackers(xenagos, combat);
        AssertJUnit.assertTrue(combat.getAttackers().isEmpty());
    }
}
