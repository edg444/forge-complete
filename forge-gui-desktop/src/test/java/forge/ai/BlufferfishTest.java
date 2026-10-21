package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Blufferfish (UNF #39), checked against Scryfall and its 2 rulings 2026-10-21: the AI opponent states a fact about its
 * own hidden cards, true or false (user, 2026-10-21); a right guess gives +2/+0 and can't be blocked this turn.
 */
public class BlufferfishTest extends AITest {

    @Test
    public void testRightGuessesPayOffSomeOfTheTime() {
        int right = 0;
        for (int i = 0; i < 30; i++) {
            final Game game = initAndCreateGame();
            final Player ai = game.getPlayers().get(1);
            final Player opp = game.getPlayers().get(0);
            addCardToZone("Forest", opp, ZoneType.Hand);
            addCardToZone("Grizzly Bears", opp, ZoneType.Library);
            final Card fish = addCard("Blufferfish", ai);
            final Card blocker = addCard("Grizzly Bears", opp);
            SpellAbility bluff = null;
            for (final SpellAbility sa : fish.getSpellAbilities()) {
                if (sa.isActivatedAbility()) {
                    bluff = sa;
                }
            }
            bluff.setActivatingPlayer(ai);
            AbilityUtils.resolve(bluff);
            game.getAction().checkStateEffects(true);
            if (fish.getNetPower() == 5) {
                right++;
                AssertJUnit.assertFalse("can't be blocked", CombatUtil.canBlock(fish, blocker));
            } else {
                AssertJUnit.assertEquals(3, fish.getNetPower());
                AssertJUnit.assertTrue(CombatUtil.canBlock(fish, blocker));
            }
        }
        AssertJUnit.assertTrue("a guess, so sometimes right and sometimes wrong: " + right, right > 0 && right < 30);
    }

    @Test
    public void testTheAisStatementsAreTrueOrFalseAsNoted() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        for (int i = 0; i < 20; i++) {
            final boolean want = i % 2 == 0;
            final String s = forge.game.ability.effects.BluffEffect.aiStatement(ai, want).text();
            AssertJUnit.assertNotNull(s);
            if (s.equals("I have a land card in my hand.") || s.equals("I have exactly 1 creature card in my hand.")) {
                AssertJUnit.assertTrue(s, want);
            }
            if (s.equals("I have no land cards in my hand.") || s.equals("I have exactly 0 creature cards in my hand.")
                    || s.equals("I have exactly 2 creature cards in my hand.")) {
                AssertJUnit.assertFalse(s, want);
            }
        }
    }
}
