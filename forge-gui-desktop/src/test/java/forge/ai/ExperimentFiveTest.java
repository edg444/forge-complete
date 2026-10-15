package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Experiment Five (CMB1 #73), checked against Scryfall 2026-10-14 (no rulings): {Z} is paid with one mana from any
 * source that could produce two or more colors of mana.
 */
public class ExperimentFiveTest extends AITest {

    @Test
    public void zParsesAsItsOwnShard() {
        ManaCost cost = new ManaCost(new ManaCostParser("1 Z"));
        AssertJUnit.assertEquals("{1}{Z}", cost.toString());
        AssertJUnit.assertEquals(2, cost.getCMC());
        AssertJUnit.assertTrue(ManaCostShard.Z.isMultiColorSource());
    }

    @Test
    public void whichSourcesCouldProduceTwoColors() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        AssertJUnit.assertFalse(addCard("Forest", ai).couldProduceTwoOrMoreColors());
        AssertJUnit.assertFalse(addCard("Sol Ring", ai).couldProduceTwoOrMoreColors());
        AssertJUnit.assertTrue(addCard("Tropical Island", ai).couldProduceTwoOrMoreColors());
        AssertJUnit.assertTrue(addCard("Karplusan Forest", ai).couldProduceTwoOrMoreColors());
        AssertJUnit.assertTrue(addCard("Birds of Paradise", ai).couldProduceTwoOrMoreColors());
        AssertJUnit.assertTrue(addCard("Azorius Signet", ai).couldProduceTwoOrMoreColors());
    }

    @Test
    public void manaFromAForestCantPayZ() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card forest = addCard("Forest", ai);
        Card dual = addCard("Tropical Island", ai);

        ManaCostBeingPaid z = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("Z")));
        AssertJUnit.assertFalse(z.isNeeded(new Mana(MagicColor.GREEN, forest, null, ai), ai.getManaPool()));
        AssertJUnit.assertTrue(z.isNeeded(new Mana(MagicColor.GREEN, dual, null, ai), ai.getManaPool()));
    }

    @Test
    public void theAiPaysZWithTheDualLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card five = addCard("Experiment Five", ai);
        addCard("Forest", ai);
        addCard("Forest", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility pump = five.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        pump.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("two Forests can't pay {Z}", ComputerUtilMana.canPayManaCost(pump, ai, 0, false));

        Card dual = addCard("Tropical Island", ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(pump, ai, 0, false));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, pump, null));
        game.getStack().resolveStack();

        AssertJUnit.assertEquals(1, five.getCounters(CounterEnumType.P1P2));
        AssertJUnit.assertEquals(2, five.getNetPower());
        AssertJUnit.assertEquals(3, five.getNetToughness());
        AssertJUnit.assertTrue(dual.isTapped());
        AssertJUnit.assertEquals("a Forest paid the {1}", 1, forge.game.card.CardLists.count(
                ai.getCardsIn(forge.game.zone.ZoneType.Battlefield), c -> c.getName().equals("Forest") && c.isTapped()));
    }
}
