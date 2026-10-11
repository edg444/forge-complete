package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Learned Learner (Mystery Booster playtest card), Oracle text checked against Scryfall 2026-10-10 (no rulings): it
 * has "{T}: Draw a card." while your maximum hand size isn't 7 - and no maximum hand size counts (user, 2026-10-10).
 * Its Cephalid type is Octopus now, as real Cephalids' are.
 */
public class LearnedLearnerTest extends AITest {

    private static boolean canDraw(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Draw) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testDrawsWithAnyOtherMaximum() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card learner = addCard("Learned Learner", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(learner.getType().hasCreatureType("Octopus"));
        AssertJUnit.assertFalse(canDraw(learner));

        Card tower = addCard("Reliquary Tower", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ai.isUnlimitedHandSize());
        AssertJUnit.assertTrue(canDraw(learner));

        // a smaller maximum counts too, and back at 7 it's gone
        game.getAction().destroy(tower, null, false, null);
        Card miser = addCard("Gnat Miser", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(6, ai.getMaxHandSize());
        AssertJUnit.assertTrue(canDraw(learner));

        game.getAction().destroy(miser, null, false, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(7, ai.getMaxHandSize());
        AssertJUnit.assertFalse(canDraw(learner));
    }
}
