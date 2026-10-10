package forge.screens.match;

import java.util.EnumSet;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.util.collect.FCollection;

/** Two-Headed Giant teams sit together: the local team in the bottom cell, the other team in the top one. */
public class GiantTeamSeatingTest extends AITest {

    private Game createGame(final GameType variant) {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        final String[] names = {"a1", "a2", "b1", "b2"};
        for (int i = 0; i < names.length; i++) {
            final RegisteredPlayer rp = new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi(names[i], null));
            rp.setTeamNumber(i / 2);
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        if (variant != null) {
            rules.setAppliedVariants(EnumSet.of(variant));
        }
        return new Game(players, rules, new Match(rules, players, "Test"));
    }

    @Test
    public void localTeamSharesTheBottomCell() {
        final List<PlayerView> seats = Lists.newArrayList(createGame(GameType.TwoHeadedGiant).getView().getPlayers());
        AssertJUnit.assertTrue(CMatchUI.hasGiantTeammates(seats));

        // fields alternate bottom (even) / top (odd) cells: a1 and a2 below, b1 and b2 above
        final PlayerView a1 = seats.get(0), a2 = seats.get(1), b1 = seats.get(2), b2 = seats.get(3);
        AssertJUnit.assertEquals(List.of(a1, b1, a2, b2),
                CMatchUI.sortPlayersForGiantTeams(new FCollection<>(seats), a1));
        AssertJUnit.assertEquals(List.of(b2, a1, b1, a2),
                CMatchUI.sortPlayersForGiantTeams(new FCollection<>(seats), b2));
    }

    @Test
    public void ordinaryTeamGameHasNoGiantTeammates() {
        AssertJUnit.assertFalse(CMatchUI.hasGiantTeammates(createGame(null).getView().getPlayers()));
    }
}
