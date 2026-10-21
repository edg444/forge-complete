package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.AdjustNumberEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Truss, Chief Engineer (UNF #181), checked against Scryfall and its rulings 2026-10-21: add or subtract X from a number
 * on a spell or permanent until end of turn, never below 1.
 */
public class TrussChiefEngineerTest extends AITest {

    @Test
    public void testAdjust() {
        AssertJUnit.assertEquals(12, AdjustNumberEffect.adjust(10, 2, true));
        AssertJUnit.assertEquals(8, AdjustNumberEffect.adjust(10, 2, false));
        AssertJUnit.assertEquals("never below 1", 1, AdjustNumberEffect.adjust(3, 5, false));
        AssertJUnit.assertEquals("a 0 can't be reduced", 0, AdjustNumberEffect.adjust(0, 1, false));
    }

    @Test
    public void testNumbersAndChanges() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card sorcerer = addCard("Prodigal Sorcerer", ai);
        final List<AdjustNumberEffect.Number> numbers = AdjustNumberEffect.numbersOn(sorcerer);
        final AdjustNumberEffect.Number damage = numbers.stream().filter(n -> "NumDmg".equals(n.param())).findFirst().orElse(null);
        AssertJUnit.assertNotNull(numbers.toString(), damage);
        AssertJUnit.assertEquals(1, damage.value());

        final Card truss = addCard("Truss, Chief Engineer", ai);
        SpellAbility hack = null;
        for (final SpellAbility sa : truss.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                hack = sa;
            }
        }
        hack.setActivatingPlayer(ai);
        AdjustNumberEffect.apply(hack, sorcerer, damage, 3);
        AssertJUnit.assertEquals("3", damage.trait().getParam("NumDmg"));
        game.getEndOfTurn().executeUntil();
        AssertJUnit.assertEquals("back at end of turn", "1", damage.trait().getParam("NumDmg"));
    }

    @Test
    public void testAiPumpsItsAttacker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card truss = addCard("Truss, Chief Engineer", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        truss.setSickness(false);
        truss.addCounterInternal(CounterType.getType("HACK"), 2, ai, false, null, null);
        for (int i = 0; i < 2; i++) {
            addCard("Island", ai);
        }
        game.getAction().checkStateEffects(true);
        SpellAbility hack = null;
        for (final SpellAbility sa : truss.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                hack = sa;
            }
        }
        hack.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(hack));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, hack, null));
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("its attacker's power plus the two hack counters removed",
                bears.getNetPower() == 4 || truss.getNetPower() == 3);
        AssertJUnit.assertEquals(0, truss.getCounters(CounterType.getType("HACK")));
    }
}
