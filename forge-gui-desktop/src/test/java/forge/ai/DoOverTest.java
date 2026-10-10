package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Do-Over (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: "Restart the
 * turn, except with Do-Over in exile." Rulings: every game action this turn is undone, nothing triggers while it's
 * undone, and undone damage or life gain counts as nothing.
 */
public class DoOverTest extends AITest {

    private static void resolve(final Card card, final Player by, final Object target) {
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(by);
        if (target instanceof Card c) {
            sa.getTargets().add(c);
        } else if (target instanceof Player p) {
            sa.getTargets().add(p);
        }
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testRestartPutsTheTurnBack() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        Card mountain = addCard("Mountain", ai);
        mountain.setTapped(true);
        Card runeclaw = addCard("Runeclaw Bear", opp);
        fillLibrary(ai, 3);
        Card island = addCardToZone("Island", ai, ZoneType.Library);
        ai.getZone(ZoneType.Library).remove(island);
        ai.getZone(ZoneType.Library).add(island, 0);
        fillLibrary(opp, 3);
        Card doOver = addCardToZone("Do-Over", ai, ZoneType.Hand);

        // opponent's turn ends, the AI's begins: that's what a restart goes back to
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, opp);
        playUntilPhase(game, PhaseType.MAIN1);
        AssertJUnit.assertEquals(ai, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertTrue(game.getPhaseHandler().canRestartTurn());
        final int turn = game.getPhaseHandler().getTurn();
        AssertJUnit.assertTrue(game.getCardState(island).isInZone(ZoneType.Hand));
        AssertJUnit.assertTrue(game.getCardState(mountain).isUntapped());

        // the turn so far: a land, a bolt, a pump and a counter, a kill, tokens
        Card drawn = game.getCardState(island);
        SpellAbility land = drawn.getFirstSpellAbility();
        land.setActivatingPlayer(ai);
        land.resolve();
        AssertJUnit.assertEquals(1, ai.getLandsPlayedThisTurn());
        resolve(addCardToZone("Lightning Bolt", ai, ZoneType.Hand), ai, opp);
        resolve(addCardToZone("Giant Growth", ai, ZoneType.Hand), ai, bears);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        AssertJUnit.assertEquals(6, bears.getNetPower());
        resolve(addCardToZone("Murder", ai, ZoneType.Hand), ai, runeclaw);
        resolve(addCardToZone("Raise the Alarm", ai, ZoneType.Hand), ai, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(17, opp.getLife());
        AssertJUnit.assertTrue(game.getCardState(runeclaw).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(2, ai.getCreaturesInPlay().filter(Card::isToken).size());

        SpellAbility restart = doOver.getFirstSpellAbility();
        restart.setActivatingPlayer(ai);
        game.getStack().add(restart);
        game.getStack().resolveStack();

        // back to the start of the same turn, Do-Over aside
        AssertJUnit.assertEquals(turn, game.getPhaseHandler().getTurn());
        AssertJUnit.assertEquals(ai, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertEquals(PhaseType.UNTAP, game.getPhaseHandler().getPhase());
        AssertJUnit.assertTrue(game.getCardState(doOver).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(20, opp.getLife());
        AssertJUnit.assertEquals(0, ai.getLandsPlayedThisTurn());
        AssertJUnit.assertTrue(game.getCardState(runeclaw).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertSame(bears, game.getCardState(bears));
        AssertJUnit.assertEquals(2, bears.getNetPower());
        AssertJUnit.assertEquals(0, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(ai.getCreaturesInPlay().filter(Card::isToken).isEmpty());
        AssertJUnit.assertEquals(game.getCardState(island), ai.getCardsIn(ZoneType.Library).getFirst());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Hand).isEmpty());
        // the untap step happened again
        AssertJUnit.assertTrue(game.getCardState(mountain).isUntapped());

        // and the turn plays out again from there: the same draw
        playUntilPhase(game, PhaseType.MAIN1);
        AssertJUnit.assertEquals(turn, game.getPhaseHandler().getTurn());
        AssertJUnit.assertTrue(game.getCardState(island).isInZone(ZoneType.Hand));
    }

    @Test
    public void testWhatCameBeforeTheTurnStillHolds() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(opp, 5);
        Card doOver = addCardToZone("Do-Over", ai, ZoneType.Hand);
        Card bears = addCard("Grizzly Bears", ai);

        // on the opponent's turn: they gain hexproof until their next turn, and the AI sets up a delayed trigger
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        resolve(addCardToZone("Blossoming Calm", opp, ZoneType.Hand), opp, null);
        bears.setSVar("TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 3");
        SpellAbility delayed = AbilityFactory.getAbility("DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You"
                + " | Execute$ TrigGain | TriggerDescription$ At the beginning of your next upkeep, you gain 3 life.", bears);
        delayed.setActivatingPlayer(ai);
        AbilityUtils.resolve(delayed);
        AssertJUnit.assertTrue(opp.hasKeyword(Keyword.HEXPROOF));

        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, opp);
        playUntilPhase(game, PhaseType.MAIN1);
        AssertJUnit.assertEquals(ai, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertEquals(23, ai.getLife());

        SpellAbility restart = doOver.getFirstSpellAbility();
        restart.setActivatingPlayer(ai);
        game.getStack().add(restart);
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(20, ai.getLife());
        AssertJUnit.assertTrue(opp.hasKeyword(Keyword.HEXPROOF));

        // the upkeep comes again, and with it the delayed trigger
        playUntilPhase(game, PhaseType.MAIN1);
        AssertJUnit.assertEquals(23, ai.getLife());
        AssertJUnit.assertTrue(opp.hasKeyword(Keyword.HEXPROOF));

        // the opponent's "until your next turn" still ends on time
        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.UPKEEP);
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
        AssertJUnit.assertFalse(opp.hasKeyword(Keyword.HEXPROOF));
    }

    @Test
    public void testAiRestartsATurnThatWentBadly() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(opp, 5);
        addCards("Island", 2, ai);
        Card angel = addCard("Serra Angel", ai);
        Card doOver = addCardToZone("Do-Over", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, opp);
        playUntilPhase(game, PhaseType.MAIN1);

        SpellAbility restart = doOver.getFirstSpellAbility();
        restart.setActivatingPlayer(ai);
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, aic.canPlaySa(restart));

        for (int i = 0; i < 3; i++) {
            resolve(addCardToZone("Lightning Bolt", opp, ZoneType.Hand), opp, ai);
        }
        resolve(addCardToZone("Murder", opp, ZoneType.Hand), opp, angel);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, aic.canPlaySa(restart));
    }
}
