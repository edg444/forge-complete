package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Portal script review and R&D's Secret Lair printed text (2026-10-23). */
public class PortalAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private int monkLifeFrom(final String set, final ZoneType from) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card monk = printing("Venerable Monk", set, p, from);
        game.getAction().checkStateEffects(true);
        int before = p.getLife();
        game.getAction().moveToPlay(monk, p, null, null);
        resolveAll(game);
        return p.getLife() - before;
    }

    @Test
    public void testVenerableMonkTriggersOnlyFromHandAsPrinted() {
        AssertJUnit.assertEquals(2, monkLifeFrom("POR", ZoneType.Hand));
        AssertJUnit.assertEquals("as printed: only from your hand", 0, monkLifeFrom("POR", ZoneType.Graveyard));
        AssertJUnit.assertEquals("Stronghold's reprint says \"comes into play\"", 2, monkLifeFrom("STH", ZoneType.Graveyard));
    }

    @Test
    public void testBlessedReversalOnlyWhenAttackedAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card reversal = printing("Blessed Reversal", "POR", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = reversal.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.checkRestrictions(reversal, p));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(sa.checkRestrictions(reversal, p));
    }
}
