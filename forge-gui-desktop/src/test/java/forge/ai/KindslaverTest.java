package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.OutsidePlayers;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Kindslaver (Unstable) - Oracle text, the seven Scryfall rulings and the Unstable FAQ entry checked 2026-10-09. The
 * person is the AI, one of three kinds at random (user ruling).
 */
public class KindslaverTest extends AITest {

    @Test
    public void aPersonFromOutsidePlaysTheirNextTurn() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(opp, 10);
        fillLibrary(me, 10);
        final PlayerController oppOwn = opp.getController();
        final Card kindslaver = addCard("Kindslaver", me);

        SpellAbility sa = null;
        for (final SpellAbility s : kindslaver.getSpellAbilities()) {
            if (s.getApi() == ApiType.ControlPlayer) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(me);
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertSame(oppOwn, opp.getController());

        playUntilNextTurn(game);
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        // someone who isn't in the game - neither the caster nor the target - is making the target's decisions
        AssertJUnit.assertTrue(opp.getController() instanceof PlayerControllerOutsidePerson);
        AssertJUnit.assertNotSame(me.getLobbyPlayer(), opp.getController().getLobbyPlayer());

        playUntilNextTurn(game);
        AssertJUnit.assertSame(oppOwn, opp.getController());
    }

    private Game turnPlayedBy(final OutsidePlayers.Style style) {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        fillLibrary(opp, 10);
        fillLibrary(me, 10);
        addCardToZone("Forest", opp, ZoneType.Hand);
        addCard("Runeclaw Bear", opp).setSickness(false);
        addCard("Runeclaw Bear", opp).setSickness(false);
        playUntilNextTurn(game);
        opp.addController(game.getNextTimestamp(), opp,
                new PlayerControllerOutsidePerson(game, opp, new LobbyPlayerAi("outsider", null), style), false);
        playUntilPhase(game, PhaseType.END_OF_TURN);
        return game;
    }

    @Test
    public void aNoviceJustSaysGo() {
        final Game game = turnPlayedBy(OutsidePlayers.Style.NOVICE);
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        AssertJUnit.assertEquals(0, opp.getLandsPlayedThisTurn());
        AssertJUnit.assertEquals(20, me.getLife());
    }

    @Test
    public void aSaboteurThrowsEverythingIn() {
        final Game game = turnPlayedBy(OutsidePlayers.Style.SABOTEUR);
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        AssertJUnit.assertEquals(0, opp.getLandsPlayedThisTurn());
        // both bears attacked, and I had nothing to block with
        AssertJUnit.assertEquals(16, me.getLife());
    }
}
