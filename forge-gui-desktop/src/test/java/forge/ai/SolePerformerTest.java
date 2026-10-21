package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Sole Performer (UNF #154), checked against Scryfall and its rulings 2026-10-21: the {T}s it adds pay for {T} instead of
 * tapping, even on a permanent that's tapped already, but not a summoning-sick one, and empty as the step ends.
 */
public class SolePerformerTest extends AITest {

    private SpellAbility activated(final Card c, final Player p) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testTapSymbols() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card performer = addCard("Sole Performer", ai);
        final Card sorcerer = addCard("Prodigal Sorcerer", ai);
        performer.setSickness(false);
        sorcerer.setSickness(false);
        game.getAction().checkStateEffects(true);

        AbilityUtils.resolve(activated(performer, ai));
        AssertJUnit.assertEquals(2, ai.getTapSymbols());

        final int life = opp.getLife();
        for (int i = 0; i < 3; i++) {
            final SpellAbility ping = activated(sorcerer, ai);
            ping.resetTargets();
            ping.getTargets().add(opp);
            AssertJUnit.assertTrue("activation " + (i + 1), ping.getPayCosts().getCostParts().get(0).canPay(ping, ai, false));
            AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, ping, null));
            if (i < 2) {
                AssertJUnit.assertTrue("a {T} from the pool, not the Sorcerer", sorcerer.isUntapped());
            }
            while (!game.getStack().isEmpty()) {
                game.getStack().resolveStack();
            }
        }
        AssertJUnit.assertTrue("the third one taps it", sorcerer.isTapped());
        AssertJUnit.assertEquals(life - 3, opp.getLife());
        AssertJUnit.assertEquals(0, ai.getTapSymbols());

        ai.addTapSymbols(1);
        final SpellAbility again = activated(sorcerer, ai);
        AssertJUnit.assertTrue("a tapped Sorcerer can still be paid for", again.getPayCosts().getCostParts().get(0).canPay(again, ai, false));
        final Card fresh = addCard("Prodigal Sorcerer", ai);
        final SpellAbility sick = activated(fresh, ai);
        AssertJUnit.assertFalse("not a summoning-sick one", sick.getPayCosts().getCostParts().get(0).canPay(sick, ai, false));

        game.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_BEGIN, null);
        AssertJUnit.assertEquals("gone as the step ends", 0, ai.getTapSymbols());
    }
}
