package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Pleasant Taxer (UNK), checked against Scryfall 2026-10-22 (no rulings): a search or an extra draw is paid for
 * each time, or it doesn't happen (user ruling).
 */
public class UnknownEventBatch34Test extends AITest {

    @Test
    public void testSearchingCostsOneMore() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card taxer = addCard("The Pleasant Taxer", ai);
        taxer.setChosenMode("Searching libraries");
        final Card forest = addCardToZone("Forest", ai, ZoneType.Library);
        final Card growth = addCardToZone("Rampant Growth", ai, ZoneType.Hand);
        final SpellAbility cast = growth.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue("no mana for the tax: no search", forest.isInZone(ZoneType.Library));
        addCard("Plains", ai);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue("paid {1}: searched", game.getCardState(forest).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testExtraDrawsCostOneMore() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card taxer = addCard("The Pleasant Taxer", ai);
        taxer.setChosenMode("Drawing a card beyond the first each turn");
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        final int drawnBefore = ai.getNumDrawnThisTurn();
        ai.drawCards(3);
        final int drawn = ai.getNumDrawnThisTurn() - drawnBefore;
        AssertJUnit.assertEquals("the first draw is free, the others go unpaid", drawnBefore == 0 ? 1 : 0, drawn);
        addCard("Plains", ai);
        final int before = ai.getNumDrawnThisTurn();
        ai.drawCards(1);
        AssertJUnit.assertEquals("paid {1}: drew", before + 1, ai.getNumDrawnThisTurn());
    }

    @Test
    public void testCastingCreatureSpellsCostsOneMore() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card taxer = addCard("The Pleasant Taxer", ai);
        taxer.setChosenMode("Casting creature spells");
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final SpellAbility cast = bears.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(3, forge.game.cost.CostAdjustment.adjust(cast.getPayCosts(), cast, false).getTotalMana().getCMC());
        taxer.setChosenMode("Casting noncreature spells");
        AssertJUnit.assertEquals(2, forge.game.cost.CostAdjustment.adjust(cast.getPayCosts(), cast, false).getTotalMana().getCMC());
    }
}
