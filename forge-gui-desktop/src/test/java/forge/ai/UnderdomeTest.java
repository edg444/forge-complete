package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Underdome (UND #86), checked against Scryfall and its ruling 2026-10-20: its colored mana pays only costs a
 * silver-bordered card imposes.
 */
public class UnderdomeTest extends AITest {

    private static SpellAbility inHand(final Player p, final String name, final String set) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(ZoneType.Hand).add(c);
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void onlyUnCosts() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Underdome", ai);
        game.getAction().checkStateEffects(true);

        SpellAbility tutor = inHand(ai, "Goblin Tutor", "UND");
        AssertJUnit.assertTrue(tutor.getHostCard().isSilverBorderedOrAcorn());
        SpellAbility bolt = inHand(ai, "Lightning Bolt", "M10");
        AssertJUnit.assertTrue("a silver-bordered spell", ComputerUtilMana.canPayManaCost(tutor, ai, 0, false));
        AssertJUnit.assertFalse("a black-bordered one", ComputerUtilMana.canPayManaCost(bolt, ai, 0, false));
    }
}
