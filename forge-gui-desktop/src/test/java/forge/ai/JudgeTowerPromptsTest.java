package forge.ai;

import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.mockito.Mockito;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.judgetower.JudgeInfraction;
import forge.game.judgetower.JudgeTowerOptions;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputBlock;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputSelectCardsFromList;
import forge.gamemodes.match.input.InputSelectStackItems;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.gui.interfaces.IGuiGame;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.player.TargetSelection;

/**
 * Judge Tower's Phase 2, through the real prompts of a human seat: whatever is clicked is taken, nothing is decided or
 * marked for the player, and the judge rules when they commit to it. The prompts run as they do in the app, the game on
 * a thread of its own and the clicks on the EDT.
 */
public class JudgeTowerPromptsTest extends AITest {
    private IGuiGame gui;
    private final List<Selection> selections = new ArrayList<>();

    private Game createGame(final List<String> aiSeats) {
        return createGame(aiSeats, new JudgeTowerOptions());
    }

    private Game createGame(final List<String> aiSeats, final JudgeTowerOptions options) {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerHuman("human")));
        for (final String name : aiSeats) {
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

        gui = Mockito.mock(IGuiGame.class, invocation -> invocation.getMethod().isDefault() ? invocation.callRealMethod()
                : Mockito.RETURNS_DEFAULTS.answer(invocation));
        Mockito.doAnswer(invocation -> ((List<?>) invocation.getArgument(1)).get(0)).when(gui).getAbilityToPlay(any(), any(), any());
        selections.clear();
        Mockito.doAnswer(invocation -> selections.add(invocation.getArgument(0))).when(gui).setSelection(any());
        controller(game).setGui(gui);
        game.getJudge().checkpoint();
        return game;
    }

    private Game createGame() {
        return createGame(List.of("b", "c"));
    }

    private static Player human(final Game game) {
        return game.getPlayers().get(0);
    }

    private static Player seat(final Game game, final int n) {
        return game.getPlayers().get(n);
    }

    private static PlayerControllerHuman controller(final Game game) {
        return (PlayerControllerHuman) human(game).getController();
    }

    /**
     * Runs what asks the player on a game thread, answering each prompt that comes up on the EDT.
     * @return the infraction it ended in, if any
     */
    private JudgeInfraction ask(final Game game, final Runnable asks, final Consumer<Input> answer) throws Exception {
        final AtomicReference<JudgeInfraction> infraction = new AtomicReference<>();
        final AtomicReference<Throwable> crash = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                asks.run();
            } catch (final JudgeInfraction inf) {
                infraction.set(inf);
            } catch (final Throwable t) {
                crash.set(t);
            }
        });
        thread.setDaemon(true);
        thread.start();
        final PlayerControllerHuman c = controller(game);
        Input last = null;
        final long deadline = System.currentTimeMillis() + 60000;
        while (thread.isAlive()) {
            AssertJUnit.assertTrue("the game is stuck on " + last, System.currentTimeMillis() < deadline);
            final Input input = c.getInputQueue().getInput();
            if (input != null && input != last) {
                last = input;
                SwingUtilities.invokeAndWait(() -> answer.accept(input));
            }
            Thread.sleep(5);
        }
        if (crash.get() != null) {
            throw new AssertionError(crash.get());
        }
        return infraction.get();
    }

    /** Plays on through the phase loop until the human is out or the steps run out, passing at every other prompt. */
    private void play(final Game game, final int steps, final Consumer<Input> answer) throws Exception {
        final JudgeInfraction uncaught = ask(game, () -> {
            for (int i = 0; i < steps && !human(game).hasLost() && !game.isGameOver(); i++) {
                game.getPhaseHandler().mainLoopStep();
            }
        }, input -> {
            answer.accept(input);
            input.selectButtonOK();
        });
        AssertJUnit.assertNull(uncaught);
    }

    private static void assertLostToTheJudge(final Player player) {
        AssertJUnit.assertTrue(player.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, player.getOutcome().lossState);
    }

    private static void click(final Input input, final Card card) {
        input.selectCard(card, null, null);
    }

    @Test
    public void testAnIllegalAttackerIsTakenAndLosesTheGame() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card sick = addCard("Grizzly Bears", a);
        final List<Input> asked = new ArrayList<>();

        // it can't attack, and nothing else could: the player is asked regardless
        play(game, 30, input -> {
            asked.add(input);
            if (input instanceof InputAttack) {
                click(input, sick);
            }
        });

        AssertJUnit.assertTrue(asked.stream().anyMatch(InputAttack.class::isInstance));
        assertLostToTheJudge(a);
        AssertJUnit.assertFalse(game.isGameOver());
        AssertJUnit.assertTrue(a.getOutcome().loseConditionSpell.contains("Grizzly Bears"));
    }

    @Test
    public void testNotAttackingWithACreatureThatCannotIsFine() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        addCard("Grizzly Bears", a);

        play(game, 30, input -> { });

        AssertJUnit.assertFalse(a.hasLost());
    }

    @Test
    public void testAnAttackOnSomeoneWhoIsNotADefenderIsRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCard("Grizzly Bears", a);
        bears.setSickness(false);
        final Combat combat = new Combat(a);

        final JudgeInfraction attacked = ask(game, () -> new InputAttack(controller(game), a, combat).showAndWait(), input -> {
            input.selectPlayer(a, null);
            click(input, bears);
            input.selectButtonOK();
        });
        AssertJUnit.assertNotNull(attacked);

        // taken back in time, it's nothing
        final JudgeInfraction takenBack = ask(game, () -> new InputAttack(controller(game), a, combat).showAndWait(), input -> {
            input.selectPlayer(a, null);
            click(input, bears);
            click(input, bears);
            input.selectButtonOK();
        });
        AssertJUnit.assertNull(takenBack);
    }

    @Test
    public void testAnIllegalBlockIsTakenAndRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card drake = addCard("Wind Drake", seat(game, 1));
        final Card bears = addCard("Grizzly Bears", a);
        final Combat combat = new Combat(seat(game, 1));
        combat.addAttacker(drake, a);

        final JudgeInfraction blocked = ask(game, () -> new InputBlock(controller(game), a, combat).showAndWait(), input -> {
            click(input, bears);
            AssertJUnit.assertTrue(combat.isBlocking(bears, drake));
            input.selectButtonOK();
        });
        AssertJUnit.assertNotNull(blocked);
        AssertJUnit.assertTrue(blocked.getMessage().contains("Grizzly Bears"));
        combat.removeBlockAssignment(drake, bears);

        final JudgeInfraction takenBack = ask(game, () -> new InputBlock(controller(game), a, combat).showAndWait(), input -> {
            click(input, bears);
            click(input, bears);
            input.selectButtonOK();
        });
        AssertJUnit.assertNull(takenBack);
        AssertJUnit.assertFalse(combat.isBlocking(bears, drake));
    }

    @Test
    public void testALegalBlockIsNotRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card attacker = addCard("Hill Giant", seat(game, 1));
        final Card bears = addCard("Grizzly Bears", a);
        final Combat combat = new Combat(seat(game, 1));
        combat.addAttacker(attacker, a);

        final JudgeInfraction blocked = ask(game, () -> new InputBlock(controller(game), a, combat).showAndWait(), input -> {
            click(input, bears);
            input.selectButtonOK();
        });
        AssertJUnit.assertNull(blocked);
        AssertJUnit.assertTrue(combat.isBlocking(bears, attacker));
    }

    /** Anything can be clicked as a target. It's when the player commits to it that it counts. */
    @Test
    public void testAnIllegalTargetIsTakenAndRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card blade = addCardToZone("Doom Blade", a, ZoneType.Hand);
        final Card black = addCard("Vampire Nighthawk", seat(game, 1));
        final SpellAbility sa = blade.getFirstSpellAbility();
        sa.setActivatingPlayer(a);

        final JudgeInfraction targeted = ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false),
                input -> click(input, black));

        AssertJUnit.assertNotNull(targeted);
        AssertJUnit.assertTrue(targeted.getMessage().contains("Vampire Nighthawk"));
    }

    @Test
    public void testALegalTargetIsNotRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card blade = addCardToZone("Doom Blade", a, ZoneType.Hand);
        final Card bears = addCard("Grizzly Bears", seat(game, 1));
        final SpellAbility sa = blade.getFirstSpellAbility();
        sa.setActivatingPlayer(a);

        final JudgeInfraction targeted = ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false),
                input -> click(input, bears));

        AssertJUnit.assertNull(targeted);
        AssertJUnit.assertTrue(sa.getTargets().contains(bears));
    }

    /** Elsewhere the only target of a spell needing an opponent is filled in. Here the player has to say it, and can say it wrong. */
    @Test
    public void testNoTargetIsFilledIn() throws Exception {
        final Game game = createGame(List.of("b"));
        final Player a = human(game);
        final Card duress = addCardToZone("Duress", a, ZoneType.Hand);
        final SpellAbility sa = duress.getFirstSpellAbility();
        sa.setActivatingPlayer(a);
        final List<Input> asked = new ArrayList<>();

        final JudgeInfraction targeted = ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false),
                input -> {
                    asked.add(input);
                    input.selectPlayer(a, null);
                });

        AssertJUnit.assertTrue(asked.get(0) instanceof InputSelectTargets);
        AssertJUnit.assertNotNull(targeted);
    }

    /** A spell that has nothing legal to target still asks. Whether it could be cast at all is ruled on before that. */
    @Test
    public void testASpellWithNoLegalTargetStillAsks() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card blade = addCardToZone("Doom Blade", a, ZoneType.Hand);
        final Card black = addCard("Vampire Nighthawk", a);
        final SpellAbility sa = blade.getFirstSpellAbility();
        sa.setActivatingPlayer(a);

        final JudgeInfraction targeted = ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false),
                input -> click(input, black));

        AssertJUnit.assertNotNull(targeted);
    }

    @Test
    public void testATargetOnTheStackIsRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        Mockito.doReturn(true).when(gui).canPickStackTargets();
        final Card bears = addCardToZone("Grizzly Bears", seat(game, 1), ZoneType.Hand);
        final SpellAbility onStack = bears.getFirstSpellAbility();
        onStack.setActivatingPlayer(seat(game, 1));
        game.getStack().add(onStack);
        final Card negate = addCardToZone("Negate", a, ZoneType.Hand);
        final SpellAbility sa = negate.getFirstSpellAbility();
        sa.setActivatingPlayer(a);

        // Negate can't counter a creature spell
        final JudgeInfraction targeted = ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false),
                input -> {
                    AssertJUnit.assertTrue(input instanceof InputSelectStackItems);
                    input.selectAbility(onStack);
                });

        AssertJUnit.assertNotNull(targeted);
    }

    @Test
    public void testAChoiceFromOutsideTheListIsRuledOn() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCard("Grizzly Bears", a);
        final Card wall = addCard("Wall of Wood", a);
        final Card host = addCardToZone("Doom Blade", a, ZoneType.Hand);
        final SpellAbility sa = host.getFirstSpellAbility();
        final CardCollection valid = new CardCollection(bears);

        final JudgeInfraction chosen = ask(game, () -> new InputSelectCardsFromList(controller(game), 1, 1, valid, sa).showAndWait(),
                input -> click(input, wall));
        AssertJUnit.assertNotNull(chosen);
        AssertJUnit.assertTrue(chosen.getMessage().contains("Wall of Wood"));

        final JudgeInfraction fine = ask(game, () -> new InputSelectCardsFromList(controller(game), 1, 1, valid, sa).showAndWait(),
                input -> click(input, bears));
        AssertJUnit.assertNull(fine);
    }

    /** A prompt with no spell or ability behind it isn't part of the game (Dev Mode's are like this): it can't be got wrong. */
    @Test
    public void testAPromptWithNoAbilityBehindItStaysFiltered() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCard("Grizzly Bears", a);
        final Card wall = addCard("Wall of Wood", a);
        final InputSelectCardsFromList[] prompt = new InputSelectCardsFromList[1];

        final JudgeInfraction chosen = ask(game, () -> {
            prompt[0] = new InputSelectCardsFromList(controller(game), 1, 1, new CardCollection(bears));
            prompt[0].showAndWait();
        }, input -> {
            AssertJUnit.assertFalse(input.selectCard(wall, null, null));
            click(input, bears);
        });

        AssertJUnit.assertNull(chosen);
        AssertJUnit.assertEquals(List.of(bears), new ArrayList<>(prompt[0].getSelected()));
    }

    /** The ordinary way through it all still works: clicked, paid for from the format's mana, and resolved. */
    @Test
    public void testALegalCastGoesThrough() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCardToZone("Grizzly Bears", a, ZoneType.Hand);

        play(game, 12, input -> {
            if (input instanceof InputPassPriority && bears.isInZone(ZoneType.Hand)) {
                click(input, bears);
            }
        });

        AssertJUnit.assertFalse(a.hasLost());
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
    }

    /** With strikes to spare the player stays in, and the spell they'd started goes back as if it had never been. */
    @Test
    public void testAStrikeLeavesNoTraceOfTheIllegalCast() throws Exception {
        final JudgeTowerOptions options = new JudgeTowerOptions();
        options.strikes = 2;
        final Game game = createGame(List.of("b", "c"), options);
        final Player a = human(game);
        final Card blade = addCardToZone("Doom Blade", a, ZoneType.Hand);
        final Card black = addCard("Vampire Nighthawk", seat(game, 1));
        addCard("Grizzly Bears", seat(game, 1));
        final boolean[] tried = new boolean[1];

        final JudgeInfraction uncaught = ask(game, () -> game.getPhaseHandler().mainLoopStep(), input -> {
            if (input instanceof InputPassPriority && !tried[0]) {
                tried[0] = true;
                click(input, blade);
            } else if (input instanceof InputSelectTargets) {
                click(input, black);
            }
        });

        AssertJUnit.assertNull(uncaught);
        AssertJUnit.assertTrue(tried[0]);
        AssertJUnit.assertFalse(a.hasLost());
        AssertJUnit.assertTrue(blade.isInZone(ZoneType.Hand));
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertTrue(black.isInPlay());
        AssertJUnit.assertEquals(0, blade.getFirstSpellAbility().getTargets().size());
    }

    /** A sorcery on someone else's turn is on offer, and losing the game is what it costs. */
    @Test
    public void testAnIllegalCastIsOfferedAndLosesTheGame() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card divination = addCardToZone("Divination", a, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, seat(game, 1));
        game.getPhaseHandler().onStackResolved();
        game.getJudge().checkpoint();

        play(game, 6, input -> {
            if (input instanceof InputPassPriority) {
                AssertJUnit.assertNotNull(input.getActivateAction(divination));
                click(input, divination);
            }
        });

        assertLostToTheJudge(a);
        AssertJUnit.assertTrue(game.getStack().isEmpty());
    }

    @Test
    public void testCastingFromTheGraveyardWithoutAWayToIsOfferedAndLosesTheGame() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCardToZone("Grizzly Bears", a, ZoneType.Graveyard);

        play(game, 1, input -> {
            if (input instanceof InputPassPriority) {
                click(input, bears);
            }
        });

        assertLostToTheJudge(a);
    }

    @Test
    public void testActivatingAnotherPlayersPermanentLosesTheGame() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card elves = addCard("Llanowar Elves", seat(game, 1));
        elves.setSickness(false);

        play(game, 1, input -> {
            if (input instanceof InputPassPriority) {
                click(input, elves);
            }
        });

        assertLostToTheJudge(a);
    }

    /** Clicking a permanent means using it, not casting it again: one with no abilities just does nothing. */
    @Test
    public void testAPermanentIsNotCastAgain() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card bears = addCard("Grizzly Bears", a);

        play(game, 1, input -> {
            if (input instanceof InputPassPriority) {
                AssertJUnit.assertNull(input.getActivateAction(bears));
                AssertJUnit.assertFalse(input.selectCard(bears, null, null));
            }
        });

        AssertJUnit.assertFalse(a.hasLost());
    }

    @Test
    public void testNothingIsMarkedAsLegal() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        final Card blade = addCardToZone("Doom Blade", a, ZoneType.Hand);
        addCard("Grizzly Bears", seat(game, 1));
        final SpellAbility sa = blade.getFirstSpellAbility();
        sa.setActivatingPlayer(a);

        ask(game, () -> new TargetSelection(controller(game), sa).chooseTargets(null, null, null, false, false), Input::selectButtonCancel);

        AssertJUnit.assertFalse(selections.isEmpty());
        for (final Selection selection : selections) {
            AssertJUnit.assertTrue(selection.cards().isEmpty());
            AssertJUnit.assertTrue(selection.targets().isEmpty());
            AssertJUnit.assertTrue(selection.actionable().isEmpty());
        }
    }

    /** Set to skip the phase and with nothing to do, the player is still the one to pass. */
    @Test
    public void testNoPassIsMadeUnasked() throws Exception {
        final Game game = createGame();
        Mockito.doReturn(true).when(gui).isUiSetToSkipPhase(any(), any());
        final List<Input> asked = new ArrayList<>();

        play(game, 1, asked::add);

        AssertJUnit.assertTrue(asked.get(0) instanceof InputPassPriority);
    }

    /** End Turn is a pass the player asked for: it isn't held up for what they had to do, and it's judged like any pass. */
    @Test
    public void testEndTurnIsJudgedLikeAnyPass() throws Exception {
        final Game game = createGame();
        final Player a = human(game);
        addCardToZone("Grizzly Bears", a, ZoneType.Hand);

        final JudgeInfraction uncaught = ask(game, () -> game.getPhaseHandler().mainLoopStep(), Input::selectButtonCancel);

        AssertJUnit.assertNull(uncaught);
        assertLostToTheJudge(a);
    }
}
