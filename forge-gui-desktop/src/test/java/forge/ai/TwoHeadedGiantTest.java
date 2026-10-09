package forge.ai;

import java.util.EnumSet;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.mulligan.MulliganService;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Two-Headed Giant, against CR 805 (shared team turns) and 810 as of the 2026-09-25 Comprehensive Rules.
 */
public class TwoHeadedGiantTest extends AITest {

    /** a1 + a2 against b1 + b2, seated in that order, team A's first main phase. */
    private Game createTwoHeadedGiantGame() {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        final Deck deck = new Deck();
        final String[] names = {"a1", "a2", "b1", "b2"};
        for (int i = 0; i < names.length; i++) {
            final RegisteredPlayer rp = new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi(names[i], null));
            rp.setTeamNumber(i / 2);
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.TwoHeadedGiant));
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        for (final Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private Player player(final Game game, final int seat) {
        return game.getPlayers().get(seat);
    }

    @Test
    public void teamSharesThirtyLife() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1), b1 = player(game, 2);

        AssertJUnit.assertEquals(30, a1.getLife());
        AssertJUnit.assertEquals(30, b1.getLife());

        // CR 810.9: it happens to the player, the team's total moves
        a1.loseLife(5, false, false, null);
        AssertJUnit.assertEquals(25, a2.getLife());
        a2.gainLife(2, null, null);
        AssertJUnit.assertEquals(27, a1.getLife());
        AssertJUnit.assertEquals(30, b1.getLife());
        AssertJUnit.assertEquals(5, a1.getLifeLostThisTurn());
        AssertJUnit.assertEquals(0, a2.getLifeLostThisTurn());
    }

    @Test
    public void teamTakesOneTurn() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1), b1 = player(game, 2), b2 = player(game, 3);

        AssertJUnit.assertTrue(game.getPhaseHandler().isPlayerTurn(a2));
        AssertJUnit.assertFalse(game.getPhaseHandler().isPlayerTurn(b1));
        AssertJUnit.assertEquals(List.of(a1, a2), Lists.newArrayList(game.getPhaseHandler().getActivePlayers()));

        // the turn passes over a2 to the other team
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(b1, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertTrue(game.getPhaseHandler().isPlayerTurn(b2));
        AssertJUnit.assertFalse(game.getPhaseHandler().isPlayerTurn(a2));

        playUntilNextTurn(game);
        AssertJUnit.assertEquals(a1, game.getPhaseHandler().getPlayerTurn());
    }

    @Test
    public void eachHeadDrawsAndUntaps() {
        final Game game = createTwoHeadedGiantGame();
        final Player b1 = player(game, 2), b2 = player(game, 3);
        final Card land1 = addCard("Forest", b1);
        final Card land2 = addCard("Forest", b2);
        land1.setTapped(true);
        land2.setTapped(true);

        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.MAIN1);

        // CR 805.4b
        AssertJUnit.assertEquals(1, b1.getNumDrawnThisTurn());
        AssertJUnit.assertEquals(1, b2.getNumDrawnThisTurn());
        AssertJUnit.assertTrue(land1.isUntapped());
        AssertJUnit.assertTrue(land2.isUntapped());
    }

    @Test
    public void upkeepTriggersPerPlayerOnlyWhenTheyNameOne() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1), b1 = player(game, 2);
        addCard("Phyrexian Arena", a2);   // "your upkeep" - a2's upkeep is team A's
        final Card crescendo = addCard("Crescendo of War", b1); // "each upkeep" - once per turn, not once per head

        playUntilNextTurn(game);          // team B's turn
        playUntilNextTurn(game);          // team A's turn again
        playUntilPhase(game, PhaseType.DRAW);

        // one strife counter in team B's upkeep and one in team A's
        AssertJUnit.assertEquals(2, crescendo.getCounters(CounterEnumType.STRIFE));
        // Phyrexian Arena once, in team A's upkeep only - so a2 has drawn for it and for the draw step, a1 just the latter
        AssertJUnit.assertEquals(29, a1.getLife());
        AssertJUnit.assertEquals(2, a2.getNumDrawnThisTurn());
        AssertJUnit.assertEquals(1, a1.getNumDrawnThisTurn());
    }

    @Test
    public void fifteenPoisonBetweenThemLosesForBoth() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1), b1 = player(game, 2), b2 = player(game, 3);

        a1.setPoisonCounters(8, b1);
        a2.setPoisonCounters(6, b1);
        // CR 810.10a: either head counts the team's poison
        AssertJUnit.assertEquals(14, a2.getPoisonCounters());
        AssertJUnit.assertEquals(14, b1.getOpponentsTotalPoisonCounters());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(a1.hasLost());

        a2.setPoisonCounters(7, b1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(a1.hasLost());
        AssertJUnit.assertTrue(a2.hasLost());
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertFalse(b2.hasLost());
    }

    @Test
    public void proliferateGivesTheTeamOnePoison() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1), b1 = player(game, 2);
        a1.setPoisonCounters(3, b1);
        a2.setPoisonCounters(2, b1);

        final Card spell = addCardToZone("Steady Progress", b1, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(b1);
        AbilityUtils.resolve(sa);

        // b1's AI proliferates both poisoned heads, but they share one count (CR 701.34b)
        AssertJUnit.assertEquals(6, a1.getPoisonCounters());
    }

    @Test
    public void teamMulligansFinish() {
        final Game game = createTwoHeadedGiantGame();
        for (final Player p : game.getPlayers()) {
            p.drawCards(7);
        }
        new MulliganService(player(game, 0)).perform();
        for (final Player p : game.getPlayers()) {
            AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Hand).size() <= 7);
            AssertJUnit.assertFalse(p.getCardsIn(ZoneType.Hand).isEmpty());
        }
    }

    @Test
    public void oneHeadLosingLosesForTheTeam() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), a2 = player(game, 1);
        a2.getZone(ZoneType.Library).removeAllCards(true);

        a2.drawCard();
        game.getAction().checkStateEffects(true);

        // CR 810.8a
        AssertJUnit.assertTrue(a2.hasLost());
        AssertJUnit.assertTrue(a1.hasLost());
        AssertJUnit.assertTrue(game.isGameOver());
    }

    @Test
    public void defenderBlocksForTeammate() {
        final Game game = createTwoHeadedGiantGame();
        final Player a1 = player(game, 0), b1 = player(game, 2), b2 = player(game, 3);
        final Card attacker = addCard("Runeclaw Bear", a1);
        final Card blocker = addCard("Runeclaw Bear", b2);
        attacker.setSickness(false);

        final Combat combat = new Combat(a1);
        combat.addAttacker(attacker, b1);
        // CR 805.10d
        AssertJUnit.assertTrue(CombatUtil.canBlock(attacker, blocker, combat));
    }

    @Test
    public void aiTeamsPlayFullTurns() {
        final Game game = createTwoHeadedGiantGame();
        for (final Player p : game.getPlayers()) {
            for (int i = 0; i < 4; i++) {
                addCard("Forest", p);
            }
            addCard("Runeclaw Bear", p).setSickness(false);
        }
        for (int turn = 0; turn < 8 && !game.isGameOver(); turn++) {
            final Player before = game.getPhaseHandler().getPlayerTurn();
            playUntilNextTurn(game);
            if (!game.isGameOver()) {
                AssertJUnit.assertFalse(before.sharesTurnWith(game.getPhaseHandler().getPlayerTurn()));
            }
        }
        // both heads of a team always show the same life total
        AssertJUnit.assertEquals(player(game, 0).getLife(), player(game, 1).getLife());
        AssertJUnit.assertEquals(player(game, 2).getLife(), player(game, 3).getLife());
    }
}
