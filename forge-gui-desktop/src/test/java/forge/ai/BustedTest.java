package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Busted! (UNF #41), checked against Scryfall and its ruling 2026-10-21: X is the most words in a name among
 * permanents you control, counted as it resolves.
 */
public class BustedTest extends AITest {

    @Test
    public void testXIsTheLongestNameInWords() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card busted = addCardToZone("Busted!", p, ZoneType.Hand);
        addCard("Island", p);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(busted, "X", busted.getFirstSpellAbility()));
        addCard("Form of the Approach of the Second Sun", p);
        addCard("Llanowar Elves", opp);
        AssertJUnit.assertEquals("only yours, the most words", 8,
                AbilityUtils.calculateAmount(busted, "X", busted.getFirstSpellAbility()));
    }
}
