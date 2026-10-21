package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;

/**
 * Angelic Harold (UNF #162) and "Brims" Barone, Midway Mobster (UNF #163), checked against Scryfall and their rulings
 * 2026-10-21.
 */
public class UnfinityWhiteBatchTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testAngelicHaroldCountsWords() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCard("Angelic Harold", p);
        final Card bears = addCard("Grizzly Bears", p);
        final Card token = addToken("g_1_1_elf_warrior", p);
        final Card kami = addCard("Kami of the Crescent Moon", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("two words", 2, bears.getNetPower());
        AssertJUnit.assertEquals("Elf Warrior Token", 2, token.getNetPower());
        AssertJUnit.assertEquals(kami.getBasePower() + 1, kami.getNetPower());
    }

    @Test
    public void testBrimsCountersOnHats() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hatted = addCard("Grizzly Bears", ai);
        hatted.setHasHatClaim(true);
        final Card bare = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        final Card brims = addCardToZone("\"Brims\" Barone, Midway Mobster", ai, forge.game.zone.ZoneType.Hand);
        game.getAction().moveToPlay(brims, ai, null, forge.game.ability.AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals(1, hatted.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("the AI counts only known hats", 0, bare.getCounters(CounterEnumType.P1P1));
    }
}
