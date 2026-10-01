package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Skull Saucer (Unstable): "Flying / When this creature enters, destroy target creature and put your head on the
 * table. Sacrifice this creature when your head stops touching the table." - Oracle text, rulings and the Unstable
 * FAQ entry checked 2026-10-01. Honor system.
 */
public class SkullSaucerTest extends AITest {

    private static SpellAbility lift(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Sacrifice) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card saucer = addCard("Skull Saucer", game.getPlayers().get(1));
        AssertJUnit.assertEquals("Flying | When this creature enters, destroy target creature and put your head on "
                + "the table. | Sacrifice this creature when your head stops touching the table.",
                String.join(" | ", saucer.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testHeadDownThenUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", opp);
        Card saucer = addCard("Skull Saucer", ai);
        game.getAction().checkStateEffects(true);

        // the enters trigger: the Hill Giant is destroyed and the AI puts its head down, so the Saucer stays
        Trigger etb = saucer.getTriggers().getFirst();
        SpellAbility sa = etb.ensureAbility().copy(saucer, ai, false);
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(giant);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(saucer.isInPlay());

        // the AI never lifts its head
        SpellAbility up = lift(saucer);
        up.setActivatingPlayer(ai);
        for (int turn = 1; turn <= 20; turn++) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, turn);
            AssertJUnit.assertFalse(SpellApiToAi.Converter.get(up).canPlayWithSubs(ai, up).willingToPlay());
        }

        // a person owning up: it's sacrificed
        AbilityUtils.resolve(up);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(saucer).isInZone(ZoneType.Graveyard));
    }
}
