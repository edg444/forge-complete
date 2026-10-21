package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Now You See Me . . . (UNF #20), checked against Scryfall and its 2 rulings 2026-10-21: mirrors are counted from
 * your seat (honor system); the AI's room is simulated (0 to 2 mirrors).
 */
public class NowYouSeeMeTest extends AITest {

    @Test
    public void testFlickersWithACounterPerMirror() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card spell = addCardToZone("Now You See Me . . .", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        final Card back = game.getCardState(bears);
        AssertJUnit.assertTrue(back.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertNotSame("a new object", bears, back);
        final int mirrors = back.getCounters(CounterEnumType.P1P1);
        AssertJUnit.assertTrue("0 to 2 mirrors in the AI's room", mirrors >= 0 && mirrors <= 2);
        AssertJUnit.assertEquals(mirrors, spell.getChosenNumber().intValue());
        AssertJUnit.assertTrue(spell.getImprintedCards().isEmpty());
    }
}
