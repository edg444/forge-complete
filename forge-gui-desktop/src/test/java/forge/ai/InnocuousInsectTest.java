package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Innocuous Insect (Mystery Booster playtest card), Oracle text checked against Scryfall 2026-10-10. Its ruling says a
 * bought-back permanent spell goes to its owner's hand instead of entering the battlefield, but the current rules
 * (CR 702.27a, 2026-09-25) only replace the spell going to the graveyard as it resolves - a permanent spell doesn't,
 * so buyback does nothing and it enters the battlefield. Rules over rulings.
 */
public class InnocuousInsectTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    @Test
    public void testBuybackDoesNothingForAPermanent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Island", 5, ai);
        fillLibrary(ai, 3);
        Card insect = addCardToZone("Innocuous Insect", ai, ZoneType.Hand);

        SpellAbility sa = insect.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        List<OptionalCostValue> buyback = GameActionUtil.getOptionalCostValues(sa);
        AssertJUnit.assertEquals(OptionalCost.Buyback, buyback.get(0).getType());
        SpellAbility bought = GameActionUtil.addOptionalCosts(sa, buyback);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, bought, null));
        resolveAll(game);

        AssertJUnit.assertTrue(game.getCardState(insect).isInZone(ZoneType.Battlefield));
        // the cast trigger drew a card
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testAiDoesNotPayForNothing() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Island", 5, ai);
        Card insect = addCardToZone("Innocuous Insect", ai, ZoneType.Hand);
        SpellAbility sa = insect.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ai.getController().chooseOptionalCosts(sa, GameActionUtil.getOptionalCostValues(sa)).isEmpty());
    }
}
