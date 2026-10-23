package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityDrawFromBottom;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 6, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest6Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    private int count(final Player p, final String name) {
        return CardLists.count(p.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals(name));
    }

    private void library(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, ZoneType.Library);
        }
    }

    @Test
    public void testPrimalClayStillWorks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card clay = addCardToZone("Primal Clay", ai, ZoneType.Hand);
        cast(game, ai, clay);
        AssertJUnit.assertTrue(game.getCardState(clay).isInPlay());
    }

    @Test
    public void testCatchOfTheDay() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card fish = addCardToZone("Catch of the Day", ai, ZoneType.Hand);
        cast(game, ai, fish);
        final Card now = game.getCardState(fish);
        AssertJUnit.assertTrue(now.isInPlay());
        final String size = now.getNetPower() + "/" + now.getNetToughness();
        AssertJUnit.assertTrue("a size: " + size, size.equals("6/2") || size.equals("4/4") || size.equals("2/6"));
        int keywords = (now.hasKeyword(Keyword.VIGILANCE) ? 1 : 0) + (now.hasKeyword(Keyword.WARD) ? 1 : 0)
                + (now.hasKeyword("Landwalk:Island") ? 1 : 0);
        AssertJUnit.assertEquals("one keyword", 1, keywords);
        AssertJUnit.assertEquals("one attack trigger", 1, now.getTriggers().size());
    }

    @Test
    public void testDuelistsConvocationInternational() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 10);
        final Card dci = addCardToZone("Duelists' Convocation International", ai, ZoneType.Hand);
        cast(game, ai, dci);
        final Card now = game.getCardState(dci);
        AssertJUnit.assertTrue("written: " + now.getWrittenDown(), now.getWrittenDown() != null
                && now.getWrittenDown().matches("[1-9][0-9]{9}"));
        now.setWrittenDown("xxxxxxxx1" + "2");
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        cast(game, ai, addCardToZone("Llanowar Elves", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("xxxxxxxxx2", now.getWrittenDown());
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        cast(game, ai, addCardToZone("Llanowar Elves", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("no uncrossed 1 left", "xxxxxxxxx2", now.getWrittenDown());
        AssertJUnit.assertFalse(game.isGameOver());
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertTrue("all ten crossed out", game.isGameOver());
        AssertJUnit.assertTrue(game.getOutcome().isWinner(ai.getRegisteredPlayer()));
    }

    @Test
    public void testHeartOfADuelist() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 5);
        AssertJUnit.assertFalse(StaticAbilityDrawFromBottom.drawsFromAnywhere(ai));
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        cast(game, ai, addCardToZone("Heart of a Duelist", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(StaticAbilityDrawFromBottom.drawsFromAnywhere(ai));
        AssertJUnit.assertFalse(StaticAbilityDrawFromBottom.drawsFromAnywhere(game.getPlayers().get(0)));
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testNoRegretsEgretLoads() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card egret = addCardToZone("No-Regrets Egret", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("2", egret.getSVar("MulliganPeek"));
        AssertJUnit.assertTrue(egret.hasKeyword(Keyword.FLYING));
    }

    @Test
    public void testSearchElemental() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card elemental = addCard("Search Elemental", ai);
        library(ai, "Forest", 4);
        cast(game, ai, addCardToZone("Opt", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("scried", 1, elemental.getCounters(CounterEnumType.P1P1));
        cast(game, ai, addCardToZone("Rampant Growth", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("searched, so scried again", 2, elemental.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testCreepyCrawlerFear() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card crawler = addCard("Creepy Crawler", ai);
        AssertJUnit.assertFalse(opp.isValid("Player.IsAfraidOfYou", ai, crawler, null));
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertFalse("a Bear isn't scary", opp.isAfraidOf(ai));
        cast(game, ai, addCardToZone("Ravenous Chupacabra", ai, ZoneType.Hand));
        AssertJUnit.assertTrue("a Horror entered", opp.isValid("Player.IsAfraidOfYou", ai, crawler, null));
        AssertJUnit.assertFalse("not afraid of itself", ai.isAfraidOf(ai));
        for (final Player p : game.getPlayers()) {
            p.onCleanupPhase();
        }
        AssertJUnit.assertFalse("for the turn", opp.isAfraidOf(ai));
    }

    @Test
    public void testNightOfTheFlyingMerfolk() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card saga = addCardToZone("Night of the Flying Merfolk", ai, ZoneType.Hand);
        cast(game, ai, saga);
        final Card now = game.getCardState(saga);
        AssertJUnit.assertTrue(now.isInPlay());
        AssertJUnit.assertEquals("bedtime story: no lore counter as it enters", 0, now.getCounters(CounterEnumType.LORE));
        AssertJUnit.assertEquals(0, count(ai, "Merfolk Token"));
        final forge.game.GameEntityCounterTable table = new forge.game.GameEntityCounterTable();
        now.addCounter(CounterEnumType.LORE, 1, ai, table);
        table.replaceCounterEffect(game, null);
        resolveAll(game);
        AssertJUnit.assertEquals(2, count(ai, "Merfolk Token"));
    }

    @Test
    public void testMapToLorthossTemple() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 5);
        final Card map = addCard("Map to Lorthos's Temple", ai);
        cast(game, ai, addCardToZone("Sol Ring", ai, ZoneType.Hand));
        cast(game, ai, addCardToZone("Sol Ring", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("checked once", "1", map.getSVar("DivingGear").replace("Number$", ""));
        cast(game, ai, addCardToZone("Coral Merfolk", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(game.getCardState(map).isInPlay());
        cast(game, ai, addCardToZone("Opt", ai, ZoneType.Hand));
        AssertJUnit.assertFalse("sacrificed", game.getCardState(map).isInPlay());
        final Card lorthos = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Lorthos, the Tidemaker")).stream().findFirst().orElse(null);
        AssertJUnit.assertNotNull(lorthos);
        AssertJUnit.assertTrue(lorthos.isToken());
    }
}
