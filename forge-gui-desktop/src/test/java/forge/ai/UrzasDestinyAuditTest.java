package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Urza's Destiny script review (2026-10-23). */
public class UrzasDestinyAuditTest extends AITest {

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testPrivateResearchCountsItsCountersAfterTheCreatureDies() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        Card bear = addCard("Grizzly Bears", p);
        Card research = addCard("Private Research", p);
        research.attachToEntity(bear, null);
        research.setCounters(CounterType.getType("PAGE"), 3);
        game.getAction().checkStateEffects(true);
        final int before = p.getCardsIn(ZoneType.Hand).size();
        game.copyLastState();
        bear.setDamage(2);
        resolveAll(game);
        AssertJUnit.assertEquals(before + 3, p.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testMarkOfFuryReturnsAtAnyEndStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bear = addCard("Grizzly Bears", p);
        Card mark = addCard("Mark of Fury", p);
        mark.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        playUntilPhase(game, PhaseType.CLEANUP);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Hand).contains(mark));
    }

    @Test
    public void testScourFindsTheExiledEnchantmentsController() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card target = addCard("Glorious Anthem", opp);
        Card copy = addCardToZone("Glorious Anthem", opp, ZoneType.Graveyard);
        Card scour = addCardToZone("Scour", p, ZoneType.Hand);
        SpellAbility sa = scour.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.setHostCard(game.getAction().moveToStack(scour, sa));
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Exile).stream().filter(c -> c.getName().equals("Glorious Anthem")).count() == 2);
    }
}
