package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Louvaq, the Aberrant (CMB1 #95), checked against Scryfall and its 7 rulings 2026-10-15: "modified" is a power,
 * toughness or ability different from the printed one, from counters or effects - not from defining *, controller,
 * color or type, and a copy only if it ends up different.
 */
public class LouvaqTest extends AITest {

    @Test
    public void whatsModified() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", ai);
        Card goyf = addCard("Tarmogoyf", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(bears.isModifiedFromPrinted());
        AssertJUnit.assertFalse("printed keywords", angel.isModifiedFromPrinted());
        AssertJUnit.assertFalse("only defining * (ruling 3)", goyf.isModifiedFromPrinted());

        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        AssertJUnit.assertTrue("a counter", bears.isModifiedFromPrinted());

        Card bears2 = addCard("Grizzly Bears", ai);
        addCard("Glorious Anthem", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("an anthem changes its power", bears2.isModifiedFromPrinted());
    }

    @Test
    public void copiesOnlyIfDifferent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card runeclaw = addCard("Runeclaw Bear", ai);
        Card giant = addCard("Hill Giant", ai);
        SpellAbility cause = bears.getFirstSpellAbility();

        runeclaw.addCloneState(CardFactory.getCloneStates(bears, runeclaw, cause), game.getNextTimestamp());
        AssertJUnit.assertFalse("a vanilla 2/2 copying a vanilla 2/2 (ruling 2)", runeclaw.isModifiedFromPrinted());
        giant.addCloneState(CardFactory.getCloneStates(bears, giant, cause), game.getNextTimestamp());
        AssertJUnit.assertTrue("a 3/3 that became a 2/2", giant.isModifiedFromPrinted());
    }

    @Test
    public void protectionAndTheEndStepCounter() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        Card louvaq = addCard("Louvaq, the Aberrant", ai);
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(CombatUtil.canBlock(louvaq, bears));

        // at the opponent's end step, the counter goes on a creature that player controls - which makes it modified
        for (Trigger t : louvaq.getTriggers()) {
            SpellAbility sa = t.ensureAbility();
            sa.setActivatingPlayer(ai);
            sa.setTriggeringObject(forge.game.ability.AbilityKey.Player, opp);
            AssertJUnit.assertTrue(sa.canTarget(bears));
            sa.getTargets().add(bears);
            AbilityUtils.resolve(sa);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertFalse("protection from modified creatures", CombatUtil.canBlock(louvaq, bears));
        AssertJUnit.assertTrue(louvaq.getZone().is(ZoneType.Battlefield));
    }
}
