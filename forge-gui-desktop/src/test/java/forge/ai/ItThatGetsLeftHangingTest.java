package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * It That Gets Left Hanging (Unstable): "When this creature enters, ask a person outside the game to high-five you. If
 * they won't, this creature gains haste until end of turn." - Oracle text, the five rulings and the Unstable FAQ entry
 * checked 2026-10-02. Honor system: the player confirms the high five, or that there was no one to ask.
 */
public class ItThatGetsLeftHangingTest extends AITest {

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card it = addCard("It That Gets Left Hanging", game.getPlayers().get(1));
        AssertJUnit.assertEquals("When this creature enters, ask a person outside the game to high-five you. If they "
                + "won't, this creature gains haste until end of turn.", it.getAbilityText().trim());
        AssertJUnit.assertEquals(5, it.getNetPower());
        AssertJUnit.assertEquals(4, it.getNetToughness());
    }

    @Test
    public void testAiHasNoOneToAsk() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card it = addCard("It That Gets Left Hanging", ai);
        game.getAction().checkStateEffects(true);

        SpellAbility etb = it.getTriggers().getFirst().ensureAbility().copy(it, ai, false);
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        // with no one around to turn down the high five, it doesn't gain haste (rulings)
        AssertJUnit.assertFalse(it.hasKeyword(Keyword.HASTE));
    }
}
