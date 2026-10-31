package forge.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.judgetower.JudgeTowerOptions;
import forge.game.judgetower.table.TableActions;
import forge.game.judgetower.table.TableSeat;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.player.LobbyPlayerHuman;

/**
 * Judge Tower played by hand: a human seat that's a script, doing to the physical table what a player would, and the
 * judge's game checking it. A Tower of Shatters, castable by nobody with no artifacts around, keeps the rest of the
 * table out of the way; tests put what they're about into both games.
 */
public class JudgeTowerTableTest extends AITest {
    private Game table;
    private TableJudge judge;
    private Player me;
    private Script seat;

    /** Does a player's duties, unless told not to, and what the test wants in their first main phase. */
    private final class Script implements TableSeat {
        boolean untaps = true;
        boolean draws = true;
        boolean overrules = false;
        int turnsLeft = 2;
        Function<Script, SpellAbility> onMain = s -> null;
        BiConsumer<Window, String> onWindow = (k, p) -> { };
        final List<String> rulings = new ArrayList<>();
        boolean mainDone;
        /** What the test is about is over once the main phase is: creatures would have to attack, and so on. */
        boolean endsAfterMain;

        @Override
        public void window(final Window kind, final String prompt) {
            final PhaseType phase = table.getPhaseHandler().getPhase();
            final boolean mine = table.getPhaseHandler().getPlayerTurn().equals(me);
            if (mine && phase == PhaseType.UNTAP && untaps) {
                for (final Card c : me.getCardsIn(ZoneType.Battlefield)) {
                    TableActions.setTapped(table, c, false);
                }
            }
            if (mine && phase == PhaseType.DRAW && draws && kind == Window.STEP) {
                final Card top = me.getCardsIn(ZoneType.Library).get(0);
                TableActions.move(table, top, ZoneType.Hand, me, null);
            }
            onWindow.accept(kind, prompt);
        }

        @Override
        public SpellAbility priority(final String prompt) {
            final PhaseType phase = table.getPhaseHandler().getPhase();
            if (table.getPhaseHandler().getPlayerTurn().equals(me) && phase == PhaseType.MAIN1) {
                if (!mainDone) {
                    final SpellAbility sa = onMain.apply(this);
                    if (sa != null) {
                        return sa;
                    }
                    mainDone = true;
                }
            } else if (endsAfterMain && mainDone) {
                table.setGameOver(GameEndReason.Draw);
                return null;
            }
            if (table.getPhaseHandler().getPlayerTurn().equals(me) && phase == PhaseType.END_OF_TURN && table.getStack().isEmpty()) {
                mainDone = false;
                if (--turnsLeft <= 0) {
                    table.setGameOver(GameEndReason.Draw);
                }
            }
            return null;
        }

        @Override
        public boolean appeal(final String reason) {
            rulings.add(reason);
            return overrules;
        }

        @Override
        public List<Integer> choose(final String prompt, final List<String> options, final int min, final int max) {
            final List<Integer> first = new ArrayList<>();
            for (int i = 0; i < Math.max(min, 1); i++) {
                first.add(i);
            }
            return first;
        }

        @Override
        public Map<Card, Integer> assignDamage(final Card attacker, final List<Card> blockers, final int damage, final boolean toDefender) {
            return Map.of(blockers.get(0), damage);
        }

        @Override
        public void stopAutoOk() {
        }
    }

    private void setUp(final JudgeTowerOptions options) {
        initAndCreateGame();
        final Deck tower = new Deck("Tower");
        tower.getMain().add("Shatter", 40);
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerHuman("human")));
        players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerAi("b", null)));
        players.add(new RegisteredPlayer(tower).setPlayer(new LobbyPlayerAi("c", null)));
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.JudgeTower));
        rules.setJudgeTowerOptions(options);
        final Match match = new Match(rules, players, "Test");
        table = match.createGame();
        me = table.getRegisteredPlayers().get(0);
        seat = new Script();
        judge = new TableJudge(p -> seat);
        match.setGameRunner(judge);
    }

    private void setUp() {
        setUp(new JudgeTowerOptions());
    }

    private void play() {
        table.getMatch().startGame(table);
    }

    private void assertLost(final Player p) {
        AssertJUnit.assertTrue("expected a loss, rulings: " + seat.rulings, p.hasLost());
        AssertJUnit.assertEquals(GameLossReason.JudgeInfraction, p.getOutcome().lossState);
    }

    private void assertNoRulings() {
        AssertJUnit.assertEquals(List.of(), seat.rulings);
        AssertJUnit.assertFalse(me.hasLost());
    }

    private SpellAbility offered(final Card c, final String prefix) {
        for (final SpellAbility sa : judge.offeredAbilities(c, me)) {
            if (sa.toString().startsWith(prefix) || prefix.isEmpty()) {
                return sa;
            }
        }
        throw new AssertionError("not offered: " + prefix + " on " + c);
    }

    /** Casts a spell the way a player does: on the stack with its targets, and the rest done by hand when it resolves. */
    private SpellAbility cast(final Card c, final Object... targets) {
        final SpellAbility sa = offered(c, "");
        TableActions.move(table, c, ZoneType.Stack, me, null);
        TableActions.pushStack(table, c, me, true, c.getName(), List.of(targets));
        return sa;
    }

    /** Only the first main phase of the human's first turn does anything, whoever goes first. */
    private void onFirstMain(final Function<Script, SpellAbility> what) {
        final boolean[] ran = new boolean[1];
        seat.endsAfterMain = true;
        seat.onMain = s -> {
            if (ran[0]) {
                return null;
            }
            ran[0] = true;
            return what.apply(s);
        };
    }

    @Test
    public void testTurnsGoByWithTheDutiesDoneByHand() {
        setUp();
        seat.turnsLeft = 3;
        play();
        assertNoRulings();
        // the Tower ran down by everyone's draws on both sides, and what the AI seats did shows on the table
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).size() >= 3);
        AssertJUnit.assertFalse(table.getRegisteredPlayers().get(1).getCardsIn(ZoneType.Hand).isEmpty());
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Library).size() < 40);
    }

    @Test
    public void testASkippedDrawLosesTheGameForWhoeverSkippedIt() {
        setUp();
        seat.draws = false;
        play();
        assertLost(me);
        AssertJUnit.assertEquals(1, seat.rulings.size());
        AssertJUnit.assertTrue(seat.rulings.get(0), seat.rulings.get(0).contains("Shatter"));
    }

    @Test
    public void testAnOverruledRulingHasTheTableStand() {
        setUp();
        seat.draws = false;
        seat.overrules = true;
        seat.turnsLeft = 1;
        play();
        AssertJUnit.assertFalse(me.hasLost());
        AssertJUnit.assertFalse(seat.rulings.isEmpty());
    }

    @Test
    public void testLeavingAPermanentTappedAfterUntapLosesTheGame() {
        setUp();
        seat.untaps = false;
        onFirstMain(s -> {
            final Card bears = judge.addToBoth("Grizzly Bears", me, ZoneType.Battlefield);
            TableActions.setTapped(table, bears, true);
            judge.tapOnEngine(bears);
            return null;
        });
        seat.endsAfterMain = false;
        seat.turnsLeft = 3;
        play();
        assertLost(me);
    }

    @Test
    public void testASpellIsCastAndCarriedOutByHand() {
        setUp();
        final Card[] bears = new Card[1];
        onFirstMain(s -> {
            bears[0] = judge.addToBoth("Grizzly Bears", me, ZoneType.Hand);
            return cast(bears[0]);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, bears[0], ZoneType.Battlefield, me, null);
            }
        };
        play();
        assertNoRulings();
        AssertJUnit.assertTrue(bears[0].isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testNotCarryingOutASpellLosesTheGame() {
        setUp();
        onFirstMain(s -> cast(judge.addToBoth("Grizzly Bears", me, ZoneType.Hand)));
        play();
        assertLost(me);
    }

    @Test
    public void testADamageSpellIsCarriedOutAndAnAiCreatureDiesOnItsOwn() {
        setUp();
        final Card[] bolt = new Card[1];
        final Card[] target = new Card[1];
        onFirstMain(s -> {
            target[0] = judge.addToBoth("Grizzly Bears", table.getRegisteredPlayers().get(1), ZoneType.Battlefield);
            bolt[0] = judge.addToBoth("Lightning Bolt", me, ZoneType.Hand);
            return cast(bolt[0], target[0]);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, bolt[0], ZoneType.Graveyard, me, null);
            }
        };
        play();
        assertNoRulings();
        AssertJUnit.assertTrue(target[0].isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAWrongEffectIsChargedToWhoeverCarriedItOut() {
        setUp();
        final Card[] bolt = new Card[1];
        final Player victim = table.getRegisteredPlayers().get(1);
        onFirstMain(s -> {
            bolt[0] = judge.addToBoth("Lightning Bolt", me, ZoneType.Hand);
            return cast(bolt[0], victim);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, bolt[0], ZoneType.Graveyard, me, null);
                // two short
                TableActions.setLife(table, victim, victim.getLife() - 1);
            }
        };
        play();
        assertLost(me);
    }

    @Test
    public void testADamageSpellOnAPlayerCarriedOutRightly() {
        setUp();
        final Card[] bolt = new Card[1];
        final Player victim = table.getRegisteredPlayers().get(1);
        onFirstMain(s -> {
            bolt[0] = judge.addToBoth("Lightning Bolt", me, ZoneType.Hand);
            return cast(bolt[0], victim);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, bolt[0], ZoneType.Graveyard, me, null);
                TableActions.setLife(table, victim, victim.getLife() - 3);
            }
        };
        final int before = victim.getLife();
        play();
        assertNoRulings();
        AssertJUnit.assertEquals(before - 3, victim.getLife());
    }

    @Test
    public void testAnIllegalTargetOnTheTableIsAnInfraction() {
        setUp();
        onFirstMain(s -> {
            final Card black = judge.addToBoth("Vampire Nighthawk", table.getRegisteredPlayers().get(1), ZoneType.Battlefield);
            // something legal to aim at, so it's the target that's wrong and not the casting
            judge.addToBoth("Grizzly Bears", table.getRegisteredPlayers().get(1), ZoneType.Battlefield);
            return cast(judge.addToBoth("Doom Blade", me, ZoneType.Hand), black);
        });
        play();
        assertLost(me);
        AssertJUnit.assertTrue(me.getOutcome().loseConditionSpell, me.getOutcome().loseConditionSpell.contains("Vampire Nighthawk"));
    }

    @Test
    public void testATriggerPutOnTheStackByHand() {
        setUp();
        final Card[] visionary = new Card[1];
        final Card[] drawn = new Card[1];
        onFirstMain(s -> {
            visionary[0] = judge.addToBoth("Elvish Visionary", me, ZoneType.Hand);
            return cast(visionary[0]);
        });
        final int[] resolves = new int[1];
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE && resolves[0]++ == 0) {
                TableActions.move(table, visionary[0], ZoneType.Battlefield, me, null);
                TableActions.pushStack(table, visionary[0], me, false, "draw a card", List.of());
            } else if (kind == TableSeat.Window.RESOLVE) {
                drawn[0] = me.getCardsIn(ZoneType.Library).get(0);
                TableActions.move(table, drawn[0], ZoneType.Hand, me, null);
            }
        };
        play();
        assertNoRulings();
        AssertJUnit.assertEquals(2, resolves[0]);
        AssertJUnit.assertNotNull(drawn[0]);
    }

    @Test
    public void testAMissedTriggerLosesTheGameForItsController() {
        setUp();
        final Card[] visionary = new Card[1];
        onFirstMain(s -> {
            visionary[0] = judge.addToBoth("Elvish Visionary", me, ZoneType.Hand);
            return cast(visionary[0]);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, visionary[0], ZoneType.Battlefield, me, null);
            }
        };
        play();
        assertLost(me);
        AssertJUnit.assertTrue(me.getOutcome().loseConditionSpell, me.getOutcome().loseConditionSpell.contains("should be on the stack"));
    }

    /** A choice in the effect is read off what the player did: which creature they sacrificed. */
    @Test
    public void testAChoiceInAnEffectIsReadOffTheTable() {
        setUp();
        final Card[] edict = new Card[1];
        final Card[] mine = new Card[2];
        onFirstMain(s -> {
            mine[0] = judge.addToBoth("Grizzly Bears", me, ZoneType.Battlefield);
            mine[1] = judge.addToBoth("Hill Giant", me, ZoneType.Battlefield);
            edict[0] = judge.addToBoth("Diabolic Edict", me, ZoneType.Hand);
            return cast(edict[0], me);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, edict[0], ZoneType.Graveyard, me, null);
                // the second of them, so that it can't be taken for the engine's first answer
                TableActions.move(table, mine[1], ZoneType.Graveyard, me, null);
            }
        };
        play();
        assertNoRulings();
        AssertJUnit.assertTrue(mine[0].isInZone(ZoneType.Battlefield));
    }

    /** The AI's part of something a human is carrying out: they ask, and it's done for them. */
    @Test
    public void testAskingAnAiForItsPart() {
        setUp();
        final Card[] edict = new Card[1];
        final Player other = table.getRegisteredPlayers().get(1);
        final Card[] theirs = new Card[1];
        onFirstMain(s -> {
            theirs[0] = judge.addToBoth("Grizzly Bears", other, ZoneType.Battlefield);
            edict[0] = judge.addToBoth("Diabolic Edict", me, ZoneType.Hand);
            return cast(edict[0], other);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, edict[0], ZoneType.Graveyard, me, null);
                AssertJUnit.assertTrue(judge.askAi());
            }
        };
        play();
        assertNoRulings();
        AssertJUnit.assertTrue(theirs[0].isInZone(ZoneType.Graveyard));
    }

    /** A shuffle: the engine takes the table's order, or the Tower would never match again. */
    @Test
    public void testATutorAndShuffleAreTakenFromTheTable() {
        setUp();
        final Card[] tutor = new Card[1];
        onFirstMain(s -> {
            tutor[0] = judge.addToBoth("Demonic Tutor", me, ZoneType.Hand);
            return cast(tutor[0]);
        });
        seat.onWindow = (kind, prompt) -> {
            if (kind == TableSeat.Window.RESOLVE) {
                TableActions.move(table, tutor[0], ZoneType.Graveyard, me, null);
                // not the top one
                TableActions.move(table, me.getCardsIn(ZoneType.Library).get(3), ZoneType.Hand, me, null);
                TableActions.shuffleTower(table, me);
            }
        };
        play();
        assertNoRulings();
    }

    @Test
    public void testAStrikePutsTheTableBack() {
        final JudgeTowerOptions options = new JudgeTowerOptions();
        options.strikes = 2;
        setUp(options);
        final Card[] bears = new Card[1];
        final boolean[] tried = new boolean[1];
        // there before the state the infraction goes back to
        seat.onWindow = (kind, prompt) -> {
            if (bears[0] == null && table.getPhaseHandler().getPlayerTurn().equals(me)) {
                bears[0] = judge.addToBoth("Grizzly Bears", table.getRegisteredPlayers().get(1), ZoneType.Battlefield);
            }
        };
        onFirstMain(s -> {
            tried[0] = true;
            // tapping something with no cost or effect behind it, then passing
            TableActions.setTapped(table, bears[0], true);
            return null;
        });
        play();
        AssertJUnit.assertTrue(tried[0]);
        AssertJUnit.assertFalse(me.hasLost());
        AssertJUnit.assertEquals(1, seat.rulings.size());
        AssertJUnit.assertFalse(bears[0].isTapped());
    }

    @Test
    public void testResettingTheTableTakesItBackAtNoCost() {
        setUp();
        final Card[] bears = new Card[1];
        onFirstMain(s -> {
            bears[0] = judge.addToBoth("Grizzly Bears", me, ZoneType.Battlefield);
            TableActions.setTapped(table, bears[0], true);
            judge.resetTable();
            return null;
        });
        play();
        assertNoRulings();
        AssertJUnit.assertFalse(bears[0].isTapped());
    }
}
