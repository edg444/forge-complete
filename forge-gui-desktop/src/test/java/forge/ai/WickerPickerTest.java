package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Wicker Picker (UNF #196), checked against Scryfall and its rulings 2026-10-21: creature spells get sticker kicker {1};
 * paying it gives a ticket counter, then a sticker may go on the spell.
 */
public class WickerPickerTest extends AITest {

    @Test
    public void testStickerKicker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Wicker Picker", ai);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        game.getAction().checkStateEffects(true);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final SpellAbility cast = bears.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final List<OptionalCostValue> options = GameActionUtil.getOptionalCostValues(cast);
        AssertJUnit.assertTrue(options.stream().anyMatch(o -> o.getType() == OptionalCost.StickerKicker1));
        AssertJUnit.assertTrue("not on noncreature spells", GameActionUtil.getOptionalCostValues(
                addCardToZone("Giant Growth", ai, ZoneType.Hand).getFirstSpellAbility()).stream()
                .noneMatch(o -> o.getType() == OptionalCost.StickerKicker1));

        final SpellAbility kicked = GameActionUtil.addOptionalCosts(cast,
                options.stream().filter(o -> o.getType() == OptionalCost.StickerKicker1).toList());
        kicked.setActivatingPlayer(ai);
        final int tickets = ai.getCounters(CounterEnumType.TICKET);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, kicked, null));
        AssertJUnit.assertTrue("{1} more paid", ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isTapped).count() == 3);
        AssertJUnit.assertTrue("the ticket, unless a sticker spent it",
                ai.getCounters(CounterEnumType.TICKET) == tickets + 1 || !game.getCardState(bears).getStickers().isEmpty());
    }
}
