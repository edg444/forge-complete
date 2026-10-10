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
 * Biting Remark (Mystery Booster playtest card), Oracle text and ruling checked against Scryfall 2026-10-10: Scrycast
 * {0} - seen while scrying, it may be revealed and cast for {0}; no additional card is looked at.
 */
public class BitingRemarkTest extends AITest {

    @Test
    public void testScrycastWhileScrying() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card island = addCardToZone("Island", ai, ZoneType.Library);
        Card remark = createCard("Biting Remark", ai);
        ai.getZone(ZoneType.Library).add(remark, 0);
        Card opt = addCardToZone("Opt", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(remark, ai.getCardsIn(ZoneType.Library).getFirst());

        SpellAbility sa = opt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);

        // cast mid-scry, then Opt's draw takes the next card
        AssertJUnit.assertTrue(game.getCardState(remark).isInZone(ZoneType.Stack));
        AssertJUnit.assertTrue(game.getCardState(island).isInZone(ZoneType.Hand));
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(remark).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testKeywordText() {
        Game game = initAndCreateGame();
        Card remark = addCard("Biting Remark", game.getPlayers().get(1));
        AssertJUnit.assertTrue(remark.getAbilityText(), remark.getAbilityText().contains(
                "Scrycast {0} (If you see this card while scrying, you may reveal it and cast it by paying {0}.)"));
    }
}
