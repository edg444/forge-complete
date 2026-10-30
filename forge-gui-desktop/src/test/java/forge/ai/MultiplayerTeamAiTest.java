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
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI in team games (team play, Two-Headed Giant, the archenemy's opponents) and free-for-alls: symmetric effects
 * weigh what teammates lose as the AI's own loss, and every opponent's side counts, not only the first one's.
 */
public class MultiplayerTeamAiTest extends AITest {

    /**
     * Seats ai, opp1, ally, opp2 (teams 0, 1, 0, 1), or each on their own without teams; the AI's second main phase
     * on the given turn.
     */
    private Game createGame(final int[] teams, final boolean giant, final int turn) {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        final Deck deck = new Deck();
        for (int i = 0; i < teams.length; i++) {
            final RegisteredPlayer rp = new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p" + i, null));
            if (teams[i] >= 0) {
                rp.setTeamNumber(teams[i]);
            }
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        if (giant) {
            rules.setAppliedVariants(EnumSet.of(GameType.TwoHeadedGiant));
        }
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        for (final Player p : game.getPlayers()) {
            for (int i = 0; i < 10; i++) {
                addCardToZone("Wastes", p, ZoneType.Library);
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, game.getPlayers().get(0), turn);
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private Game createTwoVersusTwo() {
        return createGame(new int[] {0, 1, 0, 1}, false, 12);
    }

    private AiPlayDecision decide(final Player ai, final String spell) {
        final Card card = addCardToZone(spell, ai, ZoneType.Hand);
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test
    public void wrathWeighsTeammatesCreatures() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0), opp1 = game.getPlayers().get(1), ally = game.getPlayers().get(2),
                opp2 = game.getPlayers().get(3);
        addCards("Plains", 4, ai);
        addCard("Serra Angel", opp1);
        addCard("Serra Angel", opp2);
        addCards("Serra Angel", 3, ally);
        AssertJUnit.assertNotSame("its teammate would lose more than the opponents", AiPlayDecision.WillPlay, decide(ai, "Wrath of God"));
    }

    @Test
    public void wrathWithNothingOfTheTeamsAtStake() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0);
        addCards("Plains", 4, ai);
        addCard("Serra Angel", game.getPlayers().get(1));
        addCard("Serra Angel", game.getPlayers().get(3));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, "Wrath of God"));
    }

    @Test
    public void wrathLooksPastAnEmptyFirstOpponent() {
        final Game game = createGame(new int[] {-1, -1, -1}, false, 12);
        final Player ai = game.getPlayers().get(0);
        addCards("Plains", 4, ai);
        addCards("Serra Angel", 2, game.getPlayers().get(2));
        AssertJUnit.assertEquals("the second opponent's board counts", AiPlayDecision.WillPlay, decide(ai, "Wrath of God"));
    }

    @Test
    public void acidicSoilWontKillATeammate() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0), ally = game.getPlayers().get(2);
        addCards("Mountain", 3, ai);
        addCards("Plains", 6, ally);
        addCards("Island", 5, game.getPlayers().get(1));
        addCards("Island", 5, game.getPlayers().get(3));
        ally.setLife(5, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, "Acidic Soil"));
        ally.setLife(20, null);
        AssertJUnit.assertEquals("the opponents lose more than the team", AiPlayDecision.WillPlay, decide(ai, "Acidic Soil"));
    }

    @Test
    public void acidicSoilHitsAGiantTeamOncePerHead() {
        final Game game = createGame(new int[] {0, 0, 1, 1}, true, 12);
        final Player ai = game.getPlayers().get(0), ally = game.getPlayers().get(1);
        addCards("Mountain", 3, ai);
        addCards("Plains", 6, ally);
        addCards("Island", 7, game.getPlayers().get(2));
        addCards("Island", 7, game.getPlayers().get(3));
        // the team's 9 damage comes off one shared life total
        ai.setLife(9, null);
        AssertJUnit.assertEquals(9, ally.getLife());
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, "Acidic Soil"));
        ai.setLife(30, null);
        AssertJUnit.assertEquals("14 to the other team against 9", AiPlayDecision.WillPlay, decide(ai, "Acidic Soil"));
    }

    @Test
    public void priceOfProgressSparesATeammate() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0), ally = game.getPlayers().get(2);
        addCards("Mountain", 2, ai);
        addCards("Steam Vents", 3, ally);
        addCards("Steam Vents", 2, game.getPlayers().get(1));
        addCards("Steam Vents", 2, game.getPlayers().get(3));
        ally.setLife(6, null);
        AssertJUnit.assertNotSame("6 damage would kill its teammate", AiPlayDecision.WillPlay, decide(ai, "Price of Progress"));
    }

    @Test
    public void giantHeadSeesTheOtherDefendingHeadsBlockers() {
        final Game game = createGame(new int[] {0, 0, 1, 1}, true, 3);
        final Player ai = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, 3);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        // only one defending head has a blocker, and it blocks for both (CR 805.10d)
        addCard("Serra Angel", game.getPlayers().get(3));
        game.getAction().checkStateEffects(true);
        // the defending head is picked at random among similar threats, once a turn: try enough turns to land on either
        for (int i = 0; i < 12; i++) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, 3 + i);
            final Combat combat = new Combat(ai);
            new AiAttackController(ai).declareAttackers(combat);
            AssertJUnit.assertFalse("a 2/2 into an untapped Serra Angel", combat.isAttacking(bears));
        }
    }

    @Test
    public void mainTargetHoldsForTheTurn() {
        final Game game = createGame(new int[] {-1, -1, -1, -1}, false, 5);
        final Player ai = game.getPlayers().get(0);
        final Player first = AiAttackController.choosePreferredDefenderPlayer(ai);
        for (int i = 0; i < 20; i++) {
            AssertJUnit.assertSame(first, AiAttackController.choosePreferredDefenderPlayer(ai));
        }
        // gone from the game: pick again
        first.concede();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(ai.getOpponents().contains(first));
        AssertJUnit.assertNotSame(first, AiAttackController.choosePreferredDefenderPlayer(ai));
    }

    @Test
    public void giantTeamPoisonDangerUsesFifteen() {
        final Game game = createGame(new int[] {0, 0, 1, 1}, true, 3);
        final Player ai = game.getPlayers().get(0), opp = game.getPlayers().get(2);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, 3);
        ai.setPoisonCounters(9, opp);
        final Card agent = addCard("Blighted Agent", opp);
        final Combat combat = new Combat(opp);
        combat.addAttacker(agent, ai);
        // 10 of a team's 15: not yet the danger 10 of 10 would be
        AssertJUnit.assertFalse(ComputerUtilCombat.lifeInDanger(ai, combat));
    }

    @Test
    public void secondGiantHeadLeavesItsPartnersBlockAlone() {
        final Game game = createGame(new int[] {0, 0, 1, 1}, true, 3);
        final Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, b1, 3);
        final Card attacker = addCard("Grizzly Bears", b1);
        final Card giant = addCard("Hill Giant", a1);
        final Card angel = addCard("Serra Angel", a2);
        final Combat combat = new Combat(b1);
        combat.addAttacker(attacker, a1);
        combat.addBlocker(attacker, giant);
        new AiBlockController(a2, false).assignBlockersForCombat(combat);
        AssertJUnit.assertFalse("the Hill Giant already handles the Bears", combat.isBlocking(angel));
    }

    @Test
    public void savesATeammatesCreatureFromABolt() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0), opp1 = game.getPlayers().get(1), ally = game.getPlayers().get(2);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp1, 12);
        addCard("Plains", ai);
        final Card bears = addCard("Grizzly Bears", ally);

        Card bolt = addCardToZone("Lightning Bolt", opp1, ZoneType.Hand);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(opp1);
        boltSa.getTargets().add(bears);
        bolt = game.getAction().moveToStack(bolt, boltSa);
        boltSa.setHostCard(bolt);
        game.getStack().add(boltSa);

        final Card hands = addCardToZone("Mending Hands", ai, ZoneType.Hand);
        final SpellAbility sa = hands.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getTargets().contains(bears));
    }

    @Test
    public void priceOfProgressWithTheTeamOnBasics() {
        final Game game = createTwoVersusTwo();
        final Player ai = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        addCards("Plains", 3, game.getPlayers().get(2));
        addCards("Steam Vents", 2, game.getPlayers().get(1));
        addCards("Steam Vents", 2, game.getPlayers().get(3));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, "Price of Progress"));
    }
}
