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
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Better Than One (Unstable) - Oracle text and the eight Scryfall rulings checked 2026-10-09, plus the Unstable FAQ
 * entry. The person from outside the game is played by the AI (user ruling).
 */
public class BetterThanOneTest extends AITest {

    private Player cast(final Player me) {
        final int before = me.getGame().getRegisteredPlayers().size();
        final Card spell = addCardToZone("Better Than One", me, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
        final List<Player> all = me.getGame().getRegisteredPlayers();
        AssertJUnit.assertEquals(before + 1, all.size());
        return all.get(all.size() - 1);
    }

    @Test
    public void personFromOutsideJoinsAsTeammate() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(me, 30);
        fillLibrary(opp, 30);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Forest", me, ZoneType.Hand);
        }
        addCardToZone("Runeclaw Bear", me, ZoneType.Hand);
        me.setLife(17, null);

        final Player mate = cast(me);

        // seated right after the caster, played by the AI, on the caster's side of the game
        AssertJUnit.assertEquals(List.of(opp, me, mate), Lists.newArrayList(game.getPlayers()));
        AssertJUnit.assertTrue(mate.getController() instanceof PlayerControllerAi);
        AssertJUnit.assertNotNull(mate.getRegisteredPlayer());
        AssertJUnit.assertTrue(me.sharesTurnWith(mate));
        AssertJUnit.assertTrue(opp.isOpponentOf(mate));
        AssertJUnit.assertFalse(me.isOpponentOf(mate));

        // "They won't need one" - the team's life total is the caster's
        AssertJUnit.assertEquals(17, mate.getLife());
        mate.loseLife(2, false, false, null);
        AssertJUnit.assertEquals(15, me.getLife());
        AssertJUnit.assertEquals(15, mate.getPoisonCountersToLose());

        // the AI hands over half its library and half the lands in its hand, and the newcomer owns them
        AssertJUnit.assertEquals(15, mate.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(15, me.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(2, mate.getCardsIn(ZoneType.Hand).size());
        for (final Card c : mate.getCardsIn(ZoneType.Library)) {
            AssertJUnit.assertEquals(mate, c.getOwner());
        }
        // the opponent is unaffected
        AssertJUnit.assertFalse(opp.isOnGiantTeam());
        AssertJUnit.assertEquals(20, opp.getLife());

        // it's the newcomer's turn too, and the next turn is the opponent's
        AssertJUnit.assertTrue(game.getPhaseHandler().isPlayerTurn(mate));
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        playUntilNextTurn(game);
        AssertJUnit.assertEquals(me, game.getPhaseHandler().getPlayerTurn());
        playUntilPhase(game, PhaseType.MAIN1);
        AssertJUnit.assertEquals(1, mate.getNumDrawnThisTurn());
        AssertJUnit.assertEquals(1, me.getNumDrawnThisTurn());
    }

    @Test
    public void castingOnATwoHeadedGiantTeamMakesThreeHeads() {
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
        final Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        for (final Player p : game.getPlayers()) {
            fillLibrary(p, 30);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, a1);
        game.getPhaseHandler().onStackResolved();

        final Player mate = cast(a1);

        // after the rest of the team, life total unchanged, and 20 poison now loses (CR 810.11)
        AssertJUnit.assertEquals(2, game.getPlayers().indexOf(mate));
        AssertJUnit.assertEquals(3, a2.getGiantTeam().size());
        AssertJUnit.assertEquals(30, mate.getLife());
        AssertJUnit.assertEquals(20, a1.getPoisonCountersToLose());
    }

    @Test
    public void teammateDeckingOutLosesForTheTeam() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(me, 30);
        fillLibrary(opp, 30);

        final Player mate = cast(me);
        mate.getZone(ZoneType.Library).removeAllCards(true);
        mate.drawCard();
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(mate.hasLost());
        AssertJUnit.assertTrue(me.hasLost());
        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertFalse(opp.hasLost());
    }
}
