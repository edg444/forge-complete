package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Blood Poet (CMB1 #34), checked against Scryfall and rulings 2026-10-13: spark abilities give or spend your own
 * spark counters, only as a sorcery, and only one per turn from among everything you control.
 */
public class BloodPoetTest extends AITest {

    private static SpellAbility spark(Card poet, Player p, boolean plus) {
        for (SpellAbility sa : poet.getSpellAbilities()) {
            if (sa.hasParam("Spark") && sa.getParam("CostDesc").startsWith(plus ? "+" : "−")) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        return null;
    }

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    @Test
    public void plusOneGivesYouASparkCounterAndOnlyOneSparkAbilityATurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card poet = addCard("Blood Poet", ai);
        Card second = addCard("Blood Poet", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(poet.hasKeyword(Keyword.SPARK));
        AssertJUnit.assertTrue(poet.getAbilityText().contains("+1: "));

        SpellAbility plus = spark(poet, ai, true);
        AssertJUnit.assertTrue(plus.canPlay());
        AssertJUnit.assertFalse("−3 needs three spark counters", spark(poet, ai, false).canPlay());
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, plus, null));
        resolveAll(game);
        AssertJUnit.assertEquals(1, ai.getCounters(CounterEnumType.SPARK));
        AssertJUnit.assertTrue(poet.hasKeyword(Keyword.LIFELINK));

        ai.addCounterInternal(CounterEnumType.SPARK, 2, ai, false, null, null);
        AssertJUnit.assertFalse(spark(poet, ai, true).canPlay());
        AssertJUnit.assertFalse(spark(poet, ai, false).canPlay());
        AssertJUnit.assertFalse("the limit counts every permanent you control", spark(second, ai, true).canPlay());
    }

    @Test
    public void minusThreeSpendsThreeAndGainsTheDiscardedManaValue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card poet = addCard("Blood Poet", ai);
        addCardToZone("Craw Wurm", opp, ZoneType.Hand);
        ai.addCounterInternal(CounterEnumType.SPARK, 4, ai, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility minus = spark(poet, ai, false);
        minus.resetTargets();
        minus.getTargets().add(opp);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, minus, null));
        resolveAll(game);
        AssertJUnit.assertEquals(1, ai.getCounters(CounterEnumType.SPARK));
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(26, ai.getLife());
    }

    @Test
    public void onlyAsASorcery() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card poet = addCard("Blood Poet", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(spark(poet, ai, true).canPlay());
    }

    @Test
    public void aiBanksSparkThenSpendsIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card poet = addCard("Blood Poet", ai);
        addCardToZone("Craw Wurm", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        AiController brains = ((PlayerControllerAi) ai.getController()).getAi();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, brains.canPlaySa(spark(poet, ai, true)));
        ai.addCounterInternal(CounterEnumType.SPARK, 3, ai, false, null, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, brains.canPlaySa(spark(poet, ai, false)));
        AssertJUnit.assertNotSame("with −3 ready, don't spend the turn's spark on +1",
                AiPlayDecision.WillPlay, brains.canPlaySa(spark(poet, ai, true)));
    }
}
