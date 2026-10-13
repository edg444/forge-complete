package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Queue of Beetles (CMB1 #61), checked against Scryfall and its ruling 2026-10-13: while it's on the battlefield the
 * bottom object of the stack resolves when all players pass, and nothing else about the stack changes.
 */
public class QueueOfBeetlesTest extends AITest {

    private static void cast(final Player p, final Card c, final Object target) {
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add((forge.game.GameObject) target);
        }
        p.getGame().getStack().add(sa);
    }

    @Test
    public void theFirstSpellCastResolvesFirst() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Queue of Beetles", opp);
        Card bears = addCard("Grizzly Bears", opp);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        Card growth = addCardToZone("Giant Growth", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Bolt first, Giant Growth "in response": the Bolt resolves first and the Bears die anyway
        cast(ai, bolt, bears);
        cast(opp, growth, bears);
        AssertJUnit.assertEquals(bolt, game.getStack().nextToResolve().getSourceCard());
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        game.getStack().resolveStack(); // Growth fizzles
        AssertJUnit.assertTrue(game.getStack().isEmpty());
    }

    @Test
    public void withoutItTheStackIsLastInFirstOut() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", opp);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        Card growth = addCardToZone("Giant Growth", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        cast(ai, bolt, bears);
        cast(opp, growth, bears);
        AssertJUnit.assertEquals(growth, game.getStack().nextToResolve().getSourceCard());
    }
}
