package forge.ai;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.judgetower.Judge;
import forge.game.judgetower.JudgeTowerOptions;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.JudgeTowerDecks;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Infinity;

/**
 * Judge Tower's Phase 1: the format's rules in normal play, the AI playing by force, and a human who breaks a rule
 * losing the game with the game going back to the last legal state. Rules: mtg.wiki, TheGamer's guide and Cube Cobra's
 * primer, with the user's rulings of 2026-10-30.
 */
public class JudgeTowerTest extends AITest {

    /** Four AI seats, the first one's main phase, with a Tower of 40 Plains. */
    private Game createGame(final JudgeTowerOptions options) {
        return createGame(options, List.of("a", "b", "c", "d"));
    }

    private Game createGame(final JudgeTowerOptions options, final List<String> seats) {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        for (final String name : seats) {
            players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi(name, null)));
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.JudgeTower));
        rules.setJudgeTowerOptions(options);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        fillLibrary(game.getPlayers().get(0), 40);
        for (final Player p : game.getPlayers()) {
            game.getJudge().createRulesEffect(p);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private Game createGame() {
        return createGame(new JudgeTowerOptions());
    }

    private static Player seat(final Game game, final int n) {
        return game.getPlayers().get(n);
    }

    /** Plays until the stack is empty and priority has come back around to the first seat. */
    private void playOut(final Game game) {
        for (int i = 0; i < 40 && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();
            if (game.getStack().isEmpty() && game.getPhaseHandler().getPhase() != PhaseType.MAIN1) {
                return;
            }
        }
    }

    /** A seat the judge rules on, as it would a human, that does what the supplier says at each priority. */
    private static PlayerControllerAi judgedController(final Game game, final Player p, final boolean overrule,
            final Supplier<List<SpellAbility>> actions) {
        final PlayerControllerAi controller = new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
            @Override
            public boolean isAI() {
                return false;
            }

            @Override
            public List<SpellAbility> chooseSpellAbilityToPlay() {
                return actions.get();
            }

            @Override
            public boolean appealJudgeRuling(final String reason) {
                return overrule;
            }
        };
        p.dangerouslySetController(controller);
        return controller;
    }

    @Test
    public void testPlayersShareTheTowerAndGraveyard() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        for (final Player p : game.getPlayers()) {
            AssertJUnit.assertSame(a.getZone(ZoneType.Library), p.getZone(ZoneType.Library));
            AssertJUnit.assertSame(a.getZone(ZoneType.Graveyard), p.getZone(ZoneType.Graveyard));
            AssertJUnit.assertNotSame(a, p.equals(a) ? null : p.getZone(ZoneType.Hand).getPlayer());
        }
        final JudgeTowerOptions separate = new JudgeTowerOptions();
        separate.sharedGraveyard = false;
        final Game other = createGame(separate);
        AssertJUnit.assertSame(seat(other, 0).getZone(ZoneType.Library), seat(other, 1).getZone(ZoneType.Library));
        AssertJUnit.assertNotSame(seat(other, 0).getZone(ZoneType.Graveyard), seat(other, 1).getZone(ZoneType.Graveyard));
    }

    @Test
    public void testNoLossAtZeroLifeButPoisonStillLoses() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Player b = seat(game, 1);
        a.setLife(-3, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(a.hasLost());
        b.setCounters(forge.game.card.CounterEnumType.POISON, 10, null, false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(b.hasLost());
        AssertJUnit.assertFalse(game.isGameOver());
    }

    @Test
    public void testLifeRules() {
        final JudgeTowerOptions reset = new JudgeTowerOptions();
        reset.life = JudgeTowerOptions.LifeRule.RESET;
        final Game game = createGame(reset);
        final Player a = seat(game, 0);
        a.setLife(3, null);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, a);
        game.onCleanupPhase();
        AssertJUnit.assertEquals(a.getStartingLife(), a.getLife());

        final JudgeTowerOptions infinite = new JudgeTowerOptions();
        infinite.life = JudgeTowerOptions.LifeRule.INFINITE;
        AssertJUnit.assertTrue(Infinity.isEitherInfinite(seat(createGame(infinite), 0).getLife()));

        final Game tracked = createGame();
        final Player t = seat(tracked, 0);
        t.setLife(3, null);
        tracked.onCleanupPhase();
        AssertJUnit.assertEquals(3, t.getLife());
    }

    @Test
    public void testAiCastsWhatItHolds() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);
        // nothing to pay it with but the format's mana
        AssertJUnit.assertEquals(1, game.getJudge().mandatoryActions(a).size());
        playOut(game);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
    }

    @Test
    public void testXIsAlwaysFive() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Blaze", a, ZoneType.Hand);
        final int before = game.getPlayers().stream().mapToInt(Player::getLife).sum();
        playOut(game);
        AssertJUnit.assertEquals(before - 5, game.getPlayers().stream().mapToInt(Player::getLife).sum());
    }

    @Test
    public void testManaAbilityOncePerTurn() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Card elves = addCard("Llanowar Elves", a);
        elves.setSickness(false);
        final Judge judge = game.getJudge();
        AssertJUnit.assertEquals(1, judge.mandatoryActions(a).size());
        // the AI activates it at once, and it isn't asked to again this turn even once untapped
        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertTrue(elves.isTapped());
        elves.untap();
        AssertJUnit.assertTrue(judge.mandatoryActions(a).isEmpty());
    }

    @Test
    public void testAbilitiesOfACardGoBottomUp() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Card shaman = addCard("Deathrite Shaman", a);
        shaman.setSickness(false);
        addCardToZone("Plains", a, ZoneType.Graveyard);
        addCardToZone("Shock", a, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", a, ZoneType.Graveyard);
        final List<SpellAbility> must = game.getJudge().mandatoryActions(a);
        AssertJUnit.assertEquals(1, must.size());
        AssertJUnit.assertTrue(must.get(0).getDescription(), must.get(0).getDescription().contains("You gain 2 life"));

        // with nothing for the bottom one to target, the next one up is what's required
        final Game game2 = createGame();
        final Player a2 = seat(game2, 0);
        addCard("Deathrite Shaman", a2).setSickness(false);
        addCardToZone("Plains", a2, ZoneType.Graveyard);
        addCardToZone("Shock", a2, ZoneType.Graveyard);
        final List<SpellAbility> must2 = game2.getJudge().mandatoryActions(a2);
        AssertJUnit.assertEquals(1, must2.size());
        AssertJUnit.assertTrue(must2.get(0).getDescription(), must2.get(0).getDescription().contains("loses 2 life"));
    }

    @Test
    public void testFlashbackIsCastAtOnce() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Think Twice", a, ZoneType.Hand);
        final int tower = a.getCardsIn(ZoneType.Library).size();
        playOut(game);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Think Twice", ZoneType.Exile));
        AssertJUnit.assertEquals(tower - 2, a.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testOneAndDoneHoldsTheFlashbackBack() {
        final JudgeTowerOptions options = new JudgeTowerOptions();
        options.oneAndDone = true;
        final Game game = createGame(options);
        final Player a = seat(game, 0);
        addCardToZone("Think Twice", a, ZoneType.Hand);
        playOut(game);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Think Twice", ZoneType.Graveyard));
    }

    @Test
    public void testBuybackIsPaidButDoesNotLoopForever() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Whispers of the Muse", a, ZoneType.Hand);
        final int tower = a.getCardsIn(ZoneType.Library).size();
        playOut(game);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Whispers of the Muse", ZoneType.Hand));
        AssertJUnit.assertEquals(tower - 1, a.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testOwnerIsTheLastToPlayIt() {
        final Game game = createGame();
        final Player b = seat(game, 1);
        final Card drawn = b.drawCards(1, null, forge.game.ability.AbilityKey.newMap()).getFirst();
        AssertJUnit.assertEquals(b, drawn.getOwner());
        AssertJUnit.assertEquals(b, game.getCardState(drawn).getOwner());
    }

    @Test
    public void testPassingWithSomethingToPlayLosesTheGame() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        judgedController(game, a, false, () -> null);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);
        final int tower = seat(game, 1).getCardsIn(ZoneType.Library).size();

        game.getPhaseHandler().mainLoopStep();

        AssertJUnit.assertTrue(a.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, a.getOutcome().lossState);
        AssertJUnit.assertFalse(game.isGameOver());
        AssertJUnit.assertEquals(3, game.getPlayers().size());
        // the Tower stays, though every card in it had been the first seat's
        AssertJUnit.assertEquals(tower, seat(game, 0).getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(seat(game, 0), seat(game, 0).getZone(ZoneType.Library).getPlayer());
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Grizzly Bears"));
        // and play goes on with the rest
        for (int i = 0; i < 60; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertFalse(game.isGameOver());
        AssertJUnit.assertNotSame(a, game.getPhaseHandler().getPlayerTurn());
    }

    @Test
    public void testAnInfractionGoesBackToTheLastLegalState() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Player b = seat(game, 1);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);
        // what happens between the last legal state and the infraction is undone
        judgedController(game, a, false, () -> {
            b.setLife(1, null);
            return null;
        });
        final int life = b.getLife();

        game.getPhaseHandler().mainLoopStep();

        AssertJUnit.assertTrue(a.hasLost());
        AssertJUnit.assertEquals(life, b.getLife());
    }

    @Test
    public void testPassingWithNothingToPlayIsFine() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        judgedController(game, a, false, () -> null);
        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertFalse(a.hasLost());
    }

    @Test
    public void testStrikes() {
        final JudgeTowerOptions options = new JudgeTowerOptions();
        options.strikes = 2;
        final Game game = createGame(options);
        final Player a = seat(game, 0);
        judgedController(game, a, false, () -> null);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);

        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertFalse(a.hasLost());
        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertTrue(a.hasLost());
    }

    @Test
    public void testAnOverruledRulingIsNotMadeAgain() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        judgedController(game, a, true, () -> null);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);

        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertFalse(a.hasLost());
        game.getPhaseHandler().mainLoopStep();
        AssertJUnit.assertFalse(a.hasLost());
        AssertJUnit.assertTrue(game.getJudge().mandatoryActions(a).isEmpty());
    }

    @Test
    public void testPlayingAnAbilityOutOfOrderIsAnInfraction() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Card shaman = addCard("Deathrite Shaman", a);
        shaman.setSickness(false);
        addCardToZone("Plains", a, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", a, ZoneType.Graveyard);
        final SpellAbility top = findSAWithPrefix(shaman, "{T}: Exile target land");
        AssertJUnit.assertNotNull(top);
        judgedController(game, a, false, () -> Collections.singletonList(top));

        game.getPhaseHandler().mainLoopStep();

        AssertJUnit.assertTrue(a.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, a.getOutcome().lossState);
    }

    @Test
    public void testAttackersAndBlockersAreForced() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Player b = seat(game, 1);
        final Card attacker = addCard("Grizzly Bears", a);
        attacker.setSickness(false);
        final Card blocker = addCard("Grizzly Bears", b);
        blocker.setSickness(false);
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertTrue(game.getCombat().isAttacking(attacker));
    }

    @Test
    public void testAiBlocksWithEverythingThatCan() {
        final Game game = createGame(new JudgeTowerOptions(), List.of("a", "b"));
        final Card attacker = addCard("Serra Angel", seat(game, 0));
        attacker.setSickness(false);
        final Card blocker = addCard("Vampire Nighthawk", seat(game, 1));
        blocker.setSickness(false);
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertTrue(game.getCombat().isAttacking(attacker));
        AssertJUnit.assertTrue(game.getCombat().isBlocked(attacker));
    }

    @Test
    public void testNotAttackingLosesTheGame() {
        final Game game = createGame(new JudgeTowerOptions(), List.of("a", "b"));
        final Player a = seat(game, 0);
        a.dangerouslySetController(new PlayerControllerAi(game, a, a.getLobbyPlayer()) {
            @Override
            public boolean isAI() {
                return false;
            }

            @Override
            public List<SpellAbility> chooseSpellAbilityToPlay() {
                return null;
            }

            @Override
            public void declareAttackers(final Player attacker, final forge.game.combat.Combat combat) {
                // no one attacks
            }
        });
        addCard("Grizzly Bears", a).setSickness(false);
        for (int i = 0; i < 20 && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertTrue(a.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, a.getOutcome().lossState);
        AssertJUnit.assertTrue(game.isGameOver() && game.getRegisteredPlayers().get(1).getOutcome().hasWon());
    }

    @Test
    public void testNotBlockingLosesTheGame() {
        final Game game = createGame(new JudgeTowerOptions(), List.of("a", "b"));
        final Player b = seat(game, 1);
        b.dangerouslySetController(new PlayerControllerAi(game, b, b.getLobbyPlayer()) {
            @Override
            public boolean isAI() {
                return false;
            }

            @Override
            public List<SpellAbility> chooseSpellAbilityToPlay() {
                return null;
            }

            @Override
            public void declareBlockers(final Player defender, final forge.game.combat.Combat combat) {
                // no one blocks
            }
        });
        addCard("Grizzly Bears", seat(game, 0)).setSickness(false);
        addCard("Grizzly Bears", b).setSickness(false);
        for (int i = 0; i < 20 && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertTrue(b.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, b.getOutcome().lossState);
    }

    @Test
    public void testKickerIsPaid() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Burst Lightning", a, ZoneType.Hand);
        final int before = game.getPlayers().stream().mapToInt(Player::getLife).sum();
        playOut(game);
        AssertJUnit.assertEquals(before - 4, game.getPlayers().stream().mapToInt(Player::getLife).sum());
    }

    @Test
    public void testMultikickerIsPaidAsManyTimesAsX() {
        final Game game = createGame();
        addCardToZone("Everflowing Chalice", seat(game, 0), ZoneType.Hand);
        playOut(game);
        final Card chalice = findCardWithName(game, "Everflowing Chalice");
        AssertJUnit.assertEquals(ZoneType.Battlefield, chalice.getZone().getZoneType());
        AssertJUnit.assertEquals(5, chalice.getCounters(forge.game.card.CounterEnumType.CHARGE));
    }

    @Test
    public void testAnAlternativeCostMustBeUsed() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Mulldrifter", a, ZoneType.Hand);
        final List<SpellAbility> must = game.getJudge().mandatoryActions(a);
        AssertJUnit.assertEquals(1, must.size());
        AssertJUnit.assertTrue(must.get(0).isEvoke());
    }

    @Test
    public void testEveryOptionalModeIsChosen() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        addCardToZone("Aid the Fallen", a, ZoneType.Hand);
        addCardToZone("Grizzly Bears", a, ZoneType.Graveyard);
        addCardToZone("Jace Beleren", a, ZoneType.Graveyard);
        playOut(game);
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Grizzly Bears", ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Jace Beleren", ZoneType.Graveyard));
    }

    @Test
    public void testAWrongXIsAnInfraction() {
        final Game game = createGame();
        final Player a = seat(game, 0);
        final Card blaze = addCardToZone("Blaze", a, ZoneType.Hand);
        final SpellAbility sa = blaze.getFirstSpellAbility();
        sa.setActivatingPlayer(a);
        game.getJudge().checkX(a, sa, 5, org.apache.commons.lang3.Range.of(0, 99));
        try {
            game.getJudge().checkX(a, sa, 4, org.apache.commons.lang3.Range.of(0, 99));
            AssertJUnit.fail("X of 4 went unnoticed");
        } catch (final forge.game.judgetower.JudgeInfraction expected) {
            AssertJUnit.assertSame(a, expected.getOffender());
        }
    }

    @Test
    public void testStartingHandOption() {
        final JudgeTowerOptions options = new JudgeTowerOptions();
        options.startingHand = 3;
        final Game game = createGame(options);
        for (final Player p : game.getPlayers()) {
            AssertJUnit.assertEquals(3, p.getStartingHandSize());
        }
        for (final Player p : createGame().getPlayers()) {
            AssertJUnit.assertEquals(0, p.getStartingHandSize());
        }
    }

    @Test
    public void testOptionsRoundTrip() {
        final JudgeTowerOptions o = new JudgeTowerOptions();
        o.ownership = JudgeTowerOptions.Ownership.DREW;
        o.life = JudgeTowerOptions.LifeRule.INFINITE;
        o.hidden = JudgeTowerOptions.HiddenInfo.ALL_PUBLIC;
        o.pointsToWin = 5;
        o.xValue = 2;
        o.startingHand = 7;
        o.sharedGraveyard = false;
        o.strikes = 3;
        o.oneAndDone = true;
        AssertJUnit.assertEquals(o.serialize(), JudgeTowerOptions.parse(o.serialize()).serialize());
        AssertJUnit.assertEquals(new JudgeTowerOptions().serialize(), JudgeTowerOptions.parse("junk;life=NOPE").serialize());
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setJudgeTowerOptions(o);
        AssertJUnit.assertEquals(5, rules.getGamesToWinMatch());
    }

    @Test
    public void testTheShippedTowerLoadsInFull() {
        initAndCreateGame();
        AssertJUnit.assertEquals(200, shippedTower().getMain().countAll());
        AssertJUnit.assertEquals(200, JudgeTowerDecks.selected(null).getMain().countAll());
    }

    private static Deck shippedTower() {
        return DeckSerializer.fromFile(new File(ForgeConstants.JUDGE_TOWER_DIR, "the_judges_tower.dck"));
    }

    /** Whole games out of a real Tower of tricky cards, all forced: they have to finish, without a crash. */
    @Test(timeOut = 3000000)
    public void testFullGamesFinish() {
        initAndCreateGame();
        final int games = Integer.getInteger("jt.games", 1);
        final int size = Integer.getInteger("jt.tower", 30);
        for (int g = 0; g < games; g++) {
            // AI seats never commit an infraction, so only running out of Tower ends the game: this keeps that short
            final Deck tower = new Deck("Tower");
            final List<forge.item.PaperCard> cards = Lists.newArrayList(shippedTower().getMain().toFlatList());
            Collections.shuffle(cards);
            tower.getMain().add(cards.subList(0, size));
            final List<RegisteredPlayer> players = Lists.newArrayList();
            for (final String name : List.of("a", "b", "c", "d")) {
                players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerAi(name, null)));
            }
            final GameRules rules = new GameRules(GameType.Constructed);
            rules.setAppliedVariants(EnumSet.of(GameType.JudgeTower));
            rules.setJudgeTowerOptions(new JudgeTowerOptions());
            final Match match = new Match(rules, players, "Test");
            final Game game = match.createGame();
            final long start = System.currentTimeMillis();
            match.startGame(game);
            System.out.println("JT game " + g + ": turns=" + game.getPhaseHandler().getTurn()
                    + " ms=" + (System.currentTimeMillis() - start) + " winner=" + game.getOutcome().getWinningPlayers()
                    + " towerLeft=" + game.getRegisteredPlayers().get(0).getCardsIn(ZoneType.Library).size());
            AssertJUnit.assertTrue(game.isGameOver());
        }
    }
}
