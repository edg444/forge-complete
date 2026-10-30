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
