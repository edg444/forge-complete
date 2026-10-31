package forge.gamemodes.match.input;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.mockito.Mockito;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.TableJudge;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.judgetower.JudgeTowerOptions;
import forge.game.judgetower.table.TableSeat;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.gui.interfaces.IGuiGame;
import forge.player.HumanTableSeat;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;

/**
 * Judge Tower by hand through the real prompts: clicks on the EDT, the menus they bring up answered by a mock GUI that
 * goes by what a row says, and the game on its own thread as in the app. Hotseat is the same with two human seats.
 */
public class JudgeTowerTablePromptsTest extends AITest {
    private final Deque<String> rowsToPick = new ArrayDeque<>();
    private final List<String> menuRows = new ArrayList<>();
    private IGuiGame gui;
    private Game table;

    private void setUp(final int humans) {
        initAndCreateGame();
        final Deck tower = new Deck("Tower");
        tower.getMain().add("Shatter", 30);
        final List<RegisteredPlayer> players = Lists.newArrayList();
        for (int i = 0; i < humans; i++) {
            players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerHuman("human" + i)));
        }
        for (int i = humans; i < 3; i++) {
            players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerAi("ai" + i, null)));
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.JudgeTower));
        rules.setJudgeTowerOptions(new JudgeTowerOptions());
        final Match match = new Match(rules, players, "Test");
        table = match.createGame();

        gui = Mockito.mock(IGuiGame.class, invocation -> invocation.getMethod().isDefault() ? invocation.callRealMethod()
                : Mockito.RETURNS_DEFAULTS.answer(invocation));
        Mockito.doAnswer(invocation -> {
            final List<?> rows = invocation.getArgument(1);
            rows.forEach(r -> menuRows.add(r.toString()));
            final String wanted = rowsToPick.poll();
            for (final Object r : rows) {
                if (wanted != null && r.toString().startsWith(wanted)) {
                    return r;
                }
            }
            return null;
        }).when(gui).oneOrNone(anyString(), anyList());
        for (final Player p : table.getPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman c) {
                c.setGui(gui);
            }
        }
        match.setGameRunner(new TableJudge(p -> new HumanTableSeat((PlayerControllerHuman) p.getController())));
    }

    private Player human(final int n) {
        return table.getRegisteredPlayers().get(n);
    }

    private PlayerControllerHuman controller(final int n) {
        return (PlayerControllerHuman) human(n).getController();
    }

    /** Runs the game, answering each prompt that comes up as it would be from the screen. */
    private void run(final int humans, final Consumer<InputTable> answer) throws Exception {
        final AtomicReference<Throwable> crash = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                table.getMatch().startGame(table);
            } catch (final Throwable t) {
                crash.set(t);
            }
        });
        thread.setDaemon(true);
        thread.start();
        Input last = null;
        final long deadline = System.currentTimeMillis() + 120000;
        while (thread.isAlive()) {
            AssertJUnit.assertTrue("stuck on " + last, System.currentTimeMillis() < deadline);
            for (int i = 0; i < humans; i++) {
                final Input input = controller(i).getInputQueue().getInput();
                if (input != null && input != last && input instanceof InputTable table0) {
                    last = input;
                    SwingUtilities.invokeAndWait(() -> { });
                    answer.accept(table0);
                }
            }
            Thread.sleep(2);
        }
        if (crash.get() != null) {
            throw new AssertionError(crash.get());
        }
    }

    /** Clicks, then waits for whatever menu or dialog it opened to be over. */
    private static void click(final InputTable input, final Card card) throws Exception {
        SwingUtilities.invokeAndWait(() -> input.selectCard(card, null, null));
        settle(input);
    }

    private static void settle(final InputTable input) throws Exception {
        for (int i = 0; i < 2000 && input.isBusy(); i++) {
            Thread.sleep(2);
        }
        AssertJUnit.assertFalse(input.isBusy());
    }

    private static void ok(final InputTable input) throws Exception {
        SwingUtilities.invokeAndWait(input::selectButtonOK);
    }

    private static void quietly(final ThrowingAction action) {
        try {
            action.run();
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    private interface ThrowingAction {
        void run() throws Exception;
    }

    private boolean myTurnIn(final PhaseType phase, final int seat) {
        return table.getPhaseHandler().getPhase() == phase && table.getPhaseHandler().getPlayerTurn().equals(human(seat));
    }

    /** A whole spell through the menus: cast from the hand, resolved by moving it where it goes. */
    @Test
    public void testCastingAndResolvingThroughTheMenus() throws Exception {
        setUp(1);
        final Card[] bears = new Card[1];
        final boolean[] cast = new boolean[1];
        final TableJudge judge = tableJudge();
        run(1, input -> quietly(() -> {
            if (myTurnIn(PhaseType.DRAW, 0) && !input.isPriorityPrompt()) {
                final Card top = human(0).getCardsIn(ZoneType.Library).get(0);
                rowsToPick.add("Into " + human(0).getName());
                click(input, top);
            }
            if (myTurnIn(PhaseType.MAIN1, 0) && input.isPriorityPrompt()) {
                if (!cast[0]) {
                    cast[0] = true;
                    bears[0] = judge.addForTest("Grizzly Bears", human(0));
                    rowsToPick.add("Play:");
                    click(input, bears[0]);
                    AssertJUnit.assertTrue(bears[0].isInZone(ZoneType.Stack));
                } else if (table.getStack().isEmpty()) {
                    // a caster keeps priority: with the spell on the stack, this is them passing it
                    table.setGameOver(GameEndReason.Draw);
                }
            }
            if (input.getWindow() == TableSeat.Window.RESOLVE) {
                rowsToPick.add("Onto the battlefield");
                click(input, bears[0]);
                AssertJUnit.assertTrue(bears[0].isInZone(ZoneType.Battlefield));
            }
            ok(input);
        }));
        AssertJUnit.assertTrue(cast[0]);
        AssertJUnit.assertTrue(bears[0].isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse(human(0).hasLost());
    }

    /** Nothing's refused, and it's the judge who says so: a draw skipped by just pressing OK. */
    @Test
    public void testASkippedDrawThroughThePrompts() throws Exception {
        setUp(1);
        Mockito.doReturn(false).when(gui).confirm(any(), anyString(), anyBoolean(), anyList());
        run(1, input -> quietly(() -> ok(input)));
        AssertJUnit.assertTrue(human(0).hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, human(0).getOutcome().lossState);
    }

    /** The table menu has what a player needs when nothing's clicked on, and everything's a row anyone can take. */
    @Test
    public void testTheTableMenuOffersEverything() throws Exception {
        setUp(1);
        final boolean[] looked = new boolean[1];
        run(1, input -> quietly(() -> {
            if (myTurnIn(PhaseType.DRAW, 0)) {
                SwingUtilities.invokeAndWait(input::selectButtonCancel);
                settle(input);
                looked[0] = true;
                table.setGameOver(GameEndReason.Draw);
            }
            ok(input);
        }));
        AssertJUnit.assertTrue(looked[0]);
        for (final String row : List.of("Draw the top card", "Shuffle the Tower", "Create a token", "Flip a coin", "Roll a die", "Announce a choice", "Put the table back")) {
            AssertJUnit.assertTrue(row + " in " + menuRows, menuRows.stream().anyMatch(r -> r.startsWith(row)));
        }
    }

    @Test
    public void testACardMenuOffersThePrimitives() throws Exception {
        setUp(1);
        final boolean[] looked = new boolean[1];
        run(1, input -> quietly(() -> {
            if (myTurnIn(PhaseType.UPKEEP, 0)) {
                final Card c = tableJudge().addForTest("Grizzly Bears", human(0));
                rowsToPick.add("Onto the battlefield");
                click(input, c);
                menuRows.clear();
                click(input, c);
                looked[0] = true;
                table.setGameOver(GameEndReason.Draw);
            }
            ok(input);
        }));
        AssertJUnit.assertTrue(looked[0]);
        for (final String row : List.of("Tap", "Counters", "Attack", "Block", "Turn face down", "Into the graveyard", "On top of the Tower", "Attach to")) {
            AssertJUnit.assertTrue(row + " in " + menuRows, menuRows.stream().anyMatch(r -> r.startsWith(row)));
        }
    }

    /** Hotseat: each seat is asked in turn, the screen going to whoever it is, and one's infraction leaves the other playing. */
    @Test
    public void testHotseatAsksEachSeatAndGoesOnWithoutTheOffender() throws Exception {
        setUp(2);
        Mockito.doReturn(false).when(gui).confirm(any(), anyString(), anyBoolean(), anyList());
        final List<PlayerControllerHuman> asked = new ArrayList<>();
        final int[] promptsAfter = new int[1];
        run(2, input -> quietly(() -> {
            final PlayerControllerHuman who = input.getController();
            asked.add(who);
            if (human(0).hasLost() || human(1).hasLost()) {
                if (++promptsAfter[0] > 4) {
                    table.setGameOver(GameEndReason.Draw);
                }
            }
            // the first seat to draw just doesn't, and is out; the other does it right from there on
            final Player me = who.getPlayer();
            final boolean someoneOut = human(0).hasLost() || human(1).hasLost();
            if (someoneOut && table.getPhaseHandler().getPhase() == PhaseType.DRAW && table.getPhaseHandler().getPlayerTurn().equals(me)
                    && input.getWindow() == TableSeat.Window.STEP) {
                rowsToPick.add("Into " + me.getName());
                click(input, me.getCardsIn(ZoneType.Library).get(0));
            }
            ok(input);
        }));
        AssertJUnit.assertTrue(asked.contains(controller(0)));
        AssertJUnit.assertTrue(asked.contains(controller(1)));
        AssertJUnit.assertTrue("lost: " + human(0).hasLost() + ", " + human(1).hasLost() + ", prompts after: " + promptsAfter[0] + " " + human(0).getOutcome() + " " + human(1).getOutcome(), human(0).hasLost() != human(1).hasLost());
        Mockito.verify(gui, Mockito.atLeast(2)).setCurrentPlayer(any());
    }

    private TableJudge tableJudge() {
        return (TableJudge) table.getMatch().getGameRunner();
    }
}
