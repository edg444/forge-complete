package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Grand Marshal Macie (UNF #168), checked against Scryfall and its rulings 2026-10-21: a held "until end of turn" effect
 * outlasts its turn while she stays tapped and ends as she untaps; pause counters cost life as they pile up.
 */
public class GrandMarshalMacieTest extends AITest {

    @Test
    public void testHoldsAnEffect() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, ai);
        final Card macie = addCard("Grand Marshal Macie", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);

        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility pump = growth.getFirstSpellAbility();
        pump.setActivatingPlayer(ai);
        pump.getTargets().add(bears);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(5, bears.getNetPower());
        AssertJUnit.assertEquals(1, game.getPausableUntils().size());

        macie.setTapped(true);
        SpellAbility hold = null;
        for (final SpellAbility sa : macie.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                hold = sa;
            }
        }
        hold.setActivatingPlayer(ai);
        AbilityUtils.resolve(hold);
        AssertJUnit.assertTrue(game.getPausableUntils().get(0).isHeldBy(macie));

        game.getEndOfTurn().executeUntil();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("the turn's over, but Macie is tapped", 5, bears.getNetPower());

        macie.untap();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("over as she untaps", 2, bears.getNetPower());
        AssertJUnit.assertTrue(game.getPausableUntils().isEmpty());
    }

    @Test
    public void testPauseCounters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card macie = addCard("Grand Marshal Macie", ai);
        final int life = ai.getLife();
        for (int i = 0; i < 2; i++) {
            final SpellAbility left = AbilityFactory.getAbility(macie.getSVar("NotUntapped"), macie);
            left.setActivatingPlayer(ai);
            AbilityUtils.resolve(left);
        }
        AssertJUnit.assertEquals(2, macie.getCounters(CounterType.getType("PAUSE")));
        AssertJUnit.assertEquals("1, then 2", life - 3, ai.getLife());
    }
}
