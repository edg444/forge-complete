package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Commander 2013 R&D's Secret Lair printed text (2026-10-23). */
public class Commander2013AuditTest extends AITest {

    /** Returns the cost of Marath's activatable charm ability. */
    private String marathCost(final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card marath = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Marath, Will of the Wild", set), p);
        p.getZone(ZoneType.Battlefield).add(marath);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : marath.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (!sa.isSpell() && sa.getRestrictions().checkOtherRestrictions(marath, sa, p)) {
                return sa.getParam("Cost");
            }
        }
        return null;
    }

    @Test
    public void testPrintedMarathAllowsXOfZeroOnlyBeforeCommanderLegends() {
        AssertJUnit.assertTrue(marathCost("C13", false).startsWith("XMin1"));
        AssertJUnit.assertFalse(marathCost("C13", true).startsWith("XMin1"));
        AssertJUnit.assertTrue(marathCost("CMR", true).startsWith("XMin1"));
        // the Magic Online promo came out after Commander Legends
        AssertJUnit.assertTrue(marathCost("PRM", true).startsWith("XMin1"));
    }
}
