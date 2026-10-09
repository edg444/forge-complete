package forge.ai;

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
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Hands;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Handy Dandy Clone Machine (Unstable) - Oracle text, the two Scryfall rulings and the Unstable FAQ entry checked
 * 2026-10-09. Real hands are tracked (user ruling): two per player, others may lend one, none means it ceases to exist.
 */
public class HandyDandyCloneMachineTest extends AITest {

    private void activate(final Card machine, final Player me) {
        SpellAbility sa = null;
        for (final SpellAbility s : machine.getSpellAbilities()) {
            if (s.getApi() == ApiType.Token) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
    }

    private CardCollection homunculi(final Game game) {
        final CardCollection result = new CardCollection();
        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().startsWith("Homunculus")) {
                result.add(c);
            }
        }
        return result;
    }

    @Test
    public void twoHandsTwoHomunculi() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card machine = addCard("Handy Dandy Clone Machine", me);

        activate(machine, me);
        activate(machine, me);
        AssertJUnit.assertEquals(2, homunculi(game).size());
        for (final Card h : homunculi(game)) {
            AssertJUnit.assertEquals(me, h.getHandLender());
            AssertJUnit.assertTrue(h.getView().getMarkerText().contains(me.getName()));
        }
        AssertJUnit.assertEquals(0, Hands.freeHands(me));

        // a third: the AI opponent won't lend a hand, and the AI can't conjure someone from outside the game
        activate(machine, me);
        AssertJUnit.assertEquals(2, homunculi(game).size());
        AssertJUnit.assertEquals(2, Hands.freeHands(opp));

        // a hand is free again once its token is gone
        game.getAction().destroy(homunculi(game).get(0), null, true, null);
        AssertJUnit.assertEquals(1, Hands.freeHands(me));
        activate(machine, me);
        AssertJUnit.assertEquals(2, homunculi(game).size());
    }

    @Test
    public void aTeammateLendsAHandAndTakesItWithThem() {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        final Deck deck = new Deck();
        final String[] names = {"opp", "me", "ally"};
        final int[] teams = {0, 1, 1};
        for (int i = 0; i < names.length; i++) {
            final RegisteredPlayer rp = new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi(names[i], null));
            rp.setTeamNumber(teams[i]);
            players.add(rp);
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        final Player me = game.getPlayers().get(1), ally = game.getPlayers().get(2);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        final Card machine = addCard("Handy Dandy Clone Machine", me);

        activate(machine, me);
        activate(machine, me);
        activate(machine, me);
        final CardCollection tokens = homunculi(game);
        AssertJUnit.assertEquals(3, tokens.size());
        final Card lent = tokens.get(2);
        AssertJUnit.assertEquals(ally, lent.getHandLender());
        AssertJUnit.assertEquals(me, lent.getController());

        // the ally leaves the game, and their hand with them
        ally.concede();
        game.getAction().checkGameOverCondition();
        AssertJUnit.assertFalse(lent.isInPlay());
        AssertJUnit.assertEquals(2, homunculi(game).size());
    }
}
