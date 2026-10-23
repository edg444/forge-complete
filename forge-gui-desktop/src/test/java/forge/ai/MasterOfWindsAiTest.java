package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * Master of Winds: "Whenever you cast an instant, sorcery, or Wizard spell, you may have this creature's base power
 * and toughness become 4/1 or 1/4 until end of turn." The AI used to decline every time (AILogic$ BestPT).
 */
public class MasterOfWindsAiTest extends AITest {

    private static SpellAbility choice(Card master) {
        for (Trigger t : master.getTriggers()) {
            if (t.getMode() == TriggerType.SpellCast) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(master.getController());
                return sa;
            }
        }
        return null;
    }

    private static boolean accepts(Player ai, SpellAbility sa) {
        return SpellApiToAi.Converter.get(sa).doTriggerNoCostWithSubs(ai, sa, false).willingToPlay();
    }

    private static String picked(Player ai, SpellAbility sa) {
        List<SpellAbility> choices = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        return SpellApiToAi.Converter.get(sa).chooseSingleSpellAbility(ai, sa, choices, null).getParam("Power");
    }

    @Test
    public void testGoesBigForAFreeAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card master = addCard("Master of Winds", ai);
        master.setSickness(false);
        addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = choice(master);
        AssertJUnit.assertTrue(accepts(ai, sa));
        AssertJUnit.assertEquals("4", picked(ai, sa));
    }

    @Test
    public void testStaysToughWhenAFlyerCouldBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card master = addCard("Master of Winds", ai);
        master.setSickness(false);
        addCard("Serra Angel", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(accepts(ai, choice(master)));
    }

    @Test
    public void testStaysToughOnTheOpponentsTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card master = addCard("Master of Winds", ai);
        master.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(accepts(ai, choice(master)));
    }

    @Test
    public void testUnblockedAttackerGoesBig() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card master = addCard("Master of Winds", ai);
        master.setSickness(false);
        addCard("Serra Angel", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        Combat combat = new Combat(ai);
        combat.addAttacker(master, opp);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = choice(master);
        AssertJUnit.assertTrue(accepts(ai, sa));
        AssertJUnit.assertEquals("4", picked(ai, sa));
    }

    @Test
    public void testBlockerPicksTheFormThatWins() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card master = addCard("Master of Winds", ai);
        Card thopter = addCard("Ornithopter", opp);
        Card bears = addCard("Grizzly Bears", opp);
        thopter.setSickness(false);
        bears.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = choice(master);

        // a 0/2: as a 4/1 it kills and lives
        Combat combat = new Combat(opp);
        combat.addAttacker(thopter, ai);
        combat.addBlocker(thopter, master);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertTrue(accepts(ai, sa));
        AssertJUnit.assertEquals("4", picked(ai, sa));

        // a 2/2: a 4/1 would trade, a 1/4 just lives - keep it
        combat = new Combat(opp);
        combat.addAttacker(bears, ai);
        combat.addBlocker(bears, master);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertFalse(accepts(ai, sa));
    }
}
