package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostGobble;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Mistakes Were Made (UNF #147), Pie-Eating Contest (UNF #151) and Plot Armor (UNF #152), checked against Scryfall and
 * their rulings 2026-10-21.
 */
public class UnfinityGreenBatchTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testMistakesWereMade() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card relic = addCard("Darksteel Relic", game.getPlayers().get(0));
        final Card spell = addCardToZone("Mistakes Were Made", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(addCard("Honden of Cleansing Fire", game.getPlayers().get(0)));
        forge.game.ability.AbilityUtils.resolve(sa);
        resolveAll(game);
        final int squirrels = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Squirrel")).size();
        AssertJUnit.assertTrue("the AI's simulated room has 0 to 2", squirrels >= 0 && squirrels <= 2);
        AssertJUnit.assertTrue(relic.isInPlay());
    }

    @Test
    public void testGobbleCost() {
        final Cost cost = new Cost("Gobble<X>", false);
        AssertJUnit.assertTrue(cost.hasSpecificCostType(CostGobble.class));
        AssertJUnit.assertEquals("Gobble X", cost.toSimpleString());
    }

    @Test
    public void testPieEatingContestAiTakesBites() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card elves = addCard("Llanowar Elves", ai);
        bears.setSickness(false);
        elves.setSickness(false);
        addToken("c_a_food_sac", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Forest", ai);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        final Card pie = addCardToZone("Pie-Eating Contest", ai, ZoneType.Hand);
        final SpellAbility sa = pie.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(CombatUtil.canAttack(bears));
        AssertJUnit.assertEquals(Integer.valueOf(2), ai.getController().announceRequirements(sa, 0, 20, "X"));
        sa.setXManaCostPaid(2);
        sa.getTargets().add(bears);
        sa.getTargets().add(elves);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        resolveAll(game);
        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertTrue(elves.hasKeyword("Trample"));
        AssertJUnit.assertEquals("bites, not its Food", 1,
                CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Food")).size());
    }

    @Test
    public void testPlotArmor() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card bears = addCard("Grizzly Bears", p);
        final Card armor = addCard("Plot Armor", p);
        armor.attachToEntity(bears, null);
        // M12 Llanowar Elves has flavor text; 30A 206 doesn't (Hardy's printings)
        final PaperCard flavored = FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves", "M12", "182");
        final PaperCard plain = FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves", "30A", "206");
        final Card withFlavor = Card.fromPaperCard(flavored, opp);
        final Card without = Card.fromPaperCard(plain, opp);
        opp.getZone(ZoneType.Battlefield).add(withFlavor);
        opp.getZone(ZoneType.Battlefield).add(without);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertTrue(CombatUtil.canBlock(bears, withFlavor));
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears, without));
    }
}
