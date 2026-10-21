package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Amped Up (UNF #98), checked against Scryfall and its 3 rulings 2026-10-21: it costs {1} less per electrical socket you
 * can see from your seat, announced as it's cast; the AI's simulated room keeps its sockets all game.
 */
public class AmpedUpTest extends AITest {

    @Test
    public void testTheSocketsMakeItCheaper() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 6; i++) {
            addCard("Mountain", ai);
        }
        final Card angel = addCard("Serra Angel", opp);
        angel.setTapped(true);
        final Card spell = addCardToZone("Amped Up", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(angel);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        final int sockets = game.getStack().peekAbility().getHostCard().getChosenNumber();
        final long untapped = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isLand() && c.isUntapped()).count();
        AssertJUnit.assertEquals("paid {1} less per socket, down to {R}", Math.min(sockets, 5), untapped);
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(ai, angel.getController());
        AssertJUnit.assertTrue(angel.isUntapped());
        AssertJUnit.assertEquals(7, angel.getNetPower());
        AssertJUnit.assertTrue(angel.hasKeyword(Keyword.HASTE));
    }
}
