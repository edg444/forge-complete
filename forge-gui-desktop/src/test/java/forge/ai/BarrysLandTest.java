package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Barry's Land (CMB1 #113), checked against Scryfall 2026-10-15 (no rulings): a basic land with the land type Cloud,
 * which has no mana ability of its own - the card's text gives {C}.
 */
public class BarrysLandTest extends AITest {

    @Test
    public void basicCloudThatTapsForColorless() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card land = addCard("Barry's Land", ai);
        AssertJUnit.assertTrue(land.isBasicLand());
        AssertJUnit.assertTrue(land.getType().hasSubtype("Cloud"));
        AssertJUnit.assertTrue(land.isValid("Land.Cloud", ai, land, null));
        AssertJUnit.assertTrue(CardType.getAllLandTypes().contains("Cloud"));
        AssertJUnit.assertFalse("not a basic land type", CardType.getBasicTypes().contains("Cloud"));

        AssertJUnit.assertEquals(1, land.getManaAbilities().size());
        SpellAbility mana = land.getManaAbilities().get(0);
        AssertJUnit.assertEquals("C", mana.getManaPart().getOrigProduced());
    }
}
