package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Teenage Mutant Ninja Turtles set review (2026-10-24). */
public class TurtlesAuditTest extends AITest {

    @Test
    public void testShreddersArmorEquipsOnlyOnceEachTurn() {
        Game game = initAndCreateGame();
        Card armor = addCardToZone("Shredder's Armor", game.getPlayers().get(1), ZoneType.Hand);
        boolean found = false;
        for (SpellAbility sa : armor.getSpellAbilities()) {
            if (sa.isEquip()) {
                found = true;
                AssertJUnit.assertEquals("1", sa.getParam("ActivationLimit"));
            }
        }
        AssertJUnit.assertTrue(found);
        String t = armor.getAbilityText();
        AssertJUnit.assertFalse(t, t.contains("ActivationLimit"));
        AssertJUnit.assertTrue(t, t.contains("Activate only once each turn"));
    }
}
