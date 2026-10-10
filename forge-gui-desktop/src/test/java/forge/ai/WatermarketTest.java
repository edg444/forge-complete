package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Watermarket (Unstable) - Oracle text checked against Scryfall 2026-10-09 (no rulings).
 */
public class WatermarketTest extends AITest {

    @Test
    public void onlyForSpellsWithWatermarks() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card market = addCard("Watermarket", me);
        final SpellAbility mana = market.getManaAbilities().get(0);
        AssertJUnit.assertEquals("2", mana.getParam("Amount"));

        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Druid of the Sacred Beaker", "UST");
        final Card druid = Card.fromPaperCard(pc, me);   // Crossbreed Labs
        me.getZone(ZoneType.Hand).add(druid);
        final Card bear = addCardToZone("Runeclaw Bear", me, ZoneType.Hand);
        druid.getFirstSpellAbility().setActivatingPlayer(me);
        bear.getFirstSpellAbility().setActivatingPlayer(me);
        AssertJUnit.assertEquals("crossbreedlabs", druid.getWatermark());
        AssertJUnit.assertTrue(bear.getWatermarks().isEmpty());

        AssertJUnit.assertTrue(mana.getManaPart().meetsManaRestrictions(druid.getFirstSpellAbility()));
        AssertJUnit.assertFalse(mana.getManaPart().meetsManaRestrictions(bear.getFirstSpellAbility()));
    }
}
