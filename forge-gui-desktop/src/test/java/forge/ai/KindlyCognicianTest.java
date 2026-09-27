package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostAdjustment;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Kindly Cognician: only the word "artifact" or "Contraption" (or a plural) in rules text counts - not reminder
 * text, the name, the type line or flavor text (Unstable ruling).
 */
public class KindlyCognicianTest extends AITest {

    private static final String REFERS = "Card.rulesTextHasWord_artifact,Card.rulesTextHasWord_Contraption";

    private static boolean refers(Card c, Player p) {
        return c.isValid(REFERS.split(","), p, c, null);
    }

    @Test
    public void testWhatCountsAsReferring() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        AssertJUnit.assertTrue(refers(addCardToZone("Shatter", me, ZoneType.Hand), me));
        AssertJUnit.assertTrue(refers(addCardToZone("Crafty Octopus", me, ZoneType.Hand), me));
        AssertJUnit.assertFalse(refers(addCardToZone("Lightning Bolt", me, ZoneType.Hand), me));
        // "nonartifact" is a different word
        AssertJUnit.assertFalse(refers(addCardToZone("Terror", me, ZoneType.Hand), me));
        // only in reminder text
        AssertJUnit.assertFalse(refers(addCardToZone("Weaponcraft Enthusiast", me, ZoneType.Hand), me));
        // only in the type line
        AssertJUnit.assertFalse(refers(addCardToZone("Ornithopter", me, ZoneType.Hand), me));
    }

    @Test
    public void testCostsOneLess() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addCard("Kindly Cognician", me);
        game.getAction().checkStateEffects(true);

        Card shatter = addCardToZone("Shatter", me, ZoneType.Hand);
        SpellAbility sa = shatter.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        ManaCostBeingPaid cost = new ManaCostBeingPaid(shatter.getManaCost());
        CostAdjustment.adjust(cost, sa, me, null, true, false);
        AssertJUnit.assertEquals(1, cost.getConvertedManaCost());

        Card bolt = addCardToZone("Lightning Bolt", me, ZoneType.Hand);
        SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(me);
        ManaCostBeingPaid boltCost = new ManaCostBeingPaid(bolt.getManaCost());
        CostAdjustment.adjust(boltCost, boltSa, me, null, true, false);
        AssertJUnit.assertEquals(1, boltCost.getConvertedManaCost());
    }
}
