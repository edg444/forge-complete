package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Squirrel-Powered Scheme (Unstable): "Increase the result of each die you roll by 2." - Oracle text checked against
 * Scryfall 2026-10-01 (no rulings).
 */
public class SquirrelPoweredSchemeTest extends AITest {

    @Test
    public void testOnlyYourRollsAndTheyAdd() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card scheme = addCard("Squirrel-Powered Scheme", me);
        AssertJUnit.assertEquals("Increase the result of each die you roll by 2.", scheme.getAbilityText().trim());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, RollDiceEffect.rollResultIncrease(me));
        AssertJUnit.assertEquals(0, RollDiceEffect.rollResultIncrease(opp));
        addCard("Squirrel-Powered Scheme", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, RollDiceEffect.rollResultIncrease(me));
    }

    @Test
    public void testOnARealRoll() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addCard("Squirrel-Powered Scheme", me);
        // Adorable Kitten: "roll a six-sided die. You gain life equal to the result."
        Card kitten = addCard("Adorable Kitten", me);
        game.getAction().checkStateEffects(true);
        SpellAbility roll = kitten.getTriggers().getFirst().ensureAbility().copy(kitten, me, false);
        roll.setActivatingPlayer(me);
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < 100; i++) {
            int before = me.getLife();
            AbilityUtils.resolve(roll);
            int gained = me.getLife() - before;
            min = Math.min(min, gained);
            max = Math.max(max, gained);
        }
        // a six-sided die plus 2: 3 to 8
        AssertJUnit.assertTrue("min " + min + " max " + max, min >= 3 && max <= 8 && max > min);
    }
}
