package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;

/**
 * Brightfield Glider: "Whenever this creature attacks while saddled, it gets +1/+2 and gains flying until end of turn."
 * Eddytrail Hawk: "Whenever this creature attacks, you may pay {E}. If you do, another target attacking creature gains
 * flying until end of turn." The AI paid the Hawk's energy for a creature that flew anyway, and left a saddled Glider
 * home as if it were a ground creature.
 */
public class SaddledGliderAiTest extends AITest {

    private static Trigger attackTrigger(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.Attacks) {
                return t;
            }
        }
        throw new AssertionError(c + " has no attack trigger");
    }

    private static SpellAbility hawkGrant(final Card hawk, final Player ai) {
        final SpellAbility sa = attackTrigger(hawk).ensureAbility();
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        return sa;
    }

    private static boolean pays(final Player ai, final SpellAbility sa) {
        return SpellApiToAi.Converter.get(sa).doTriggerNoCostWithSubs(ai, sa, false).willingToPlay();
    }

    private void attackWith(final Player ai, final Player opp, final Card... attackers) {
        final Game game = ai.getGame();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai, false);
        final Combat combat = new Combat(ai);
        for (final Card a : attackers) {
            combat.addAttacker(a, opp);
        }
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void testHawkDoesNotPayForAFlier() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card hawk = addCard("Eddytrail Hawk", ai);
        final Card crow = addCard("Storm Crow", ai);
        addCard("Grizzly Bears", opp);
        attackWith(ai, opp, hawk, crow);

        AssertJUnit.assertFalse("flying on a flier buys nothing", pays(ai, hawkGrant(hawk, ai)));
    }

    @Test
    public void testHawkDoesNotPayForTheGlidersOwnFlying() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card hawk = addCard("Eddytrail Hawk", ai);
        final Card glider = addCard("Brightfield Glider", ai);
        glider.setSaddled(true);
        addCard("Grizzly Bears", opp);
        attackWith(ai, opp, hawk, glider);

        final Trigger own = attackTrigger(glider);
        final SpellAbility pending = own.ensureAbility().copy(ai);
        pending.setTrigger(own);
        game.getStack().addSimultaneousStackEntry(new WrappedAbility(own, pending, null));

        AssertJUnit.assertFalse("the Glider's waiting trigger already gives it flying", pays(ai, hawkGrant(hawk, ai)));
    }

    @Test
    public void testHawkStillPaysForAGroundAttacker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card hawk = addCard("Eddytrail Hawk", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        attackWith(ai, opp, hawk, bears);

        final SpellAbility sa = hawkGrant(hawk, ai);
        AssertJUnit.assertTrue(pays(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    @Test
    public void testSaddledGliderAttacksOverGroundBlockers() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card glider = addCard("Brightfield Glider", ai);
        glider.setSickness(false);
        glider.setSaddled(true);
        addCards("Hill Giant", 2, opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertTrue("a saddled Glider is a 2/3 flier the Giants can't block", combat.isAttacking(glider));
    }

    @Test
    public void testUnsaddledGliderStaysHome() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card glider = addCard("Brightfield Glider", ai);
        glider.setSickness(false);
        addCards("Hill Giant", 2, opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertFalse(combat.isAttacking(glider));
    }
}
