package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/** AI misplays reported from play on 2026-11-12, each set up the way it happened. */
public class AiPlayReports20261112Test extends AITest {

    private static AiPlayDecision decide(final Player ai, final SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    private static SpellAbility triggered(final Card c, final TriggerType mode) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() == mode) {
                return t.ensureAbility();
            }
        }
        return null;
    }

    private Card ready(final String name, final Player p) {
        final Card c = addCard(name, p);
        c.setSickness(false);
        return c;
    }

    // Sorcerer Role on an attacking Howling Golem: scry, then draw the card it kept on top
    @Test
    public void testScryResolvesBeforeTheGolemsDraw() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility draw = triggered(addCard("Howling Golem", ai), TriggerType.Attacks);
        final SpellAbility scry = triggered(addCard("Augury Owl", ai), TriggerType.ChangesZone);
        AssertJUnit.assertEquals(ApiType.Draw, draw.getApi());
        AssertJUnit.assertEquals(ApiType.Scry, scry.getApi());
        final AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        for (final List<SpellAbility> in : List.of(Lists.newArrayList(draw, scry), Lists.newArrayList(scry, draw))) {
            final List<SpellAbility> out = aic.orderPlaySa(in);
            // the last one put on the stack resolves first
            AssertJUnit.assertSame(scry, out.get(out.size() - 1));
        }
    }

    // Atlantean Cavalry ("whenever you draw your second card each turn") waited for main 2, after the Golem's draw
    @Test
    public void testCavalryBeforeTheGolemAttacks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        lands(ai, "Island", 3);
        final SpellAbility cavalry = addCardToZone("Atlantean Cavalry", ai, ZoneType.Hand).getFirstSpellAbility();
        cavalry.setActivatingPlayer(ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("nothing draws in combat", ComputerUtil.castPermanentInMain1(ai, cavalry));

        ready("Howling Golem", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ComputerUtil.castPermanentInMain1(ai, cavalry));
    }

    private Card ugin(final Player opp) {
        final Card ugin = addCard("Ugin, Eye of the Storms", opp);
        ugin.addCounterInternal(CounterEnumType.LOYALTY, 9, opp, false, null, null);
        return ugin;
    }

    private Combat blocked(final Game game, final Player ai, final List<Card> attackers, final forge.game.GameEntity defender) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        final Combat combat = new Combat(ai);
        for (final Card a : attackers) {
            combat.addAttacker(a, defender);
            combat.setBlocked(a, false);
        }
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        return combat;
    }

    // two unblockable attackers on a 9-loyalty Ugin: Hatred paid 13 (the player's life less their damage) for a token on Ugin
    @Test
    public void testHatredPaysWhatThePlaneswalkerNeeds() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(15, null);
        lands(ai, "Swamp", 5);
        final Card ugin = ugin(opp);
        final List<Card> atk = List.of(ready("Phantom Warrior", ai), ready("Phantom Warrior", ai));
        final SpellAbility hatred = addCardToZone("Hatred", ai, ZoneType.Hand).getFirstSpellAbility();
        blocked(game, ai, atk, ugin);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, hatred));
        AssertJUnit.assertEquals(Integer.valueOf(5), hatred.getXManaCostPaid());
    }

    @Test
    public void testHatredFinishesThePlayer() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(8, null);
        lands(ai, "Swamp", 5);
        final Card ugin = ugin(opp);
        final Card onUgin = ready("Phantom Warrior", ai);
        final Card onOpp = ready("Phantom Warrior", ai);
        final SpellAbility hatred = addCardToZone("Hatred", ai, ZoneType.Hand).getFirstSpellAbility();
        final Combat combat = blocked(game, ai, List.of(onUgin), ugin);
        combat.addAttacker(onOpp, opp);
        combat.setBlocked(onOpp, false);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, hatred));
        AssertJUnit.assertEquals(Integer.valueOf(6), hatred.getXManaCostPaid());
        AssertJUnit.assertSame(onOpp, hatred.getTargetCard());
    }

    @Test
    public void testHatredInHandSendsTheUnblockablesAtThePlayer() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(8, null);
        lands(ai, "Swamp", 5);
        ugin(opp);
        ready("Phantom Warrior", ai);
        ready("Phantom Warrior", ai);
        addCardToZone("Hatred", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai, false);
        game.getAction().checkStateEffects(true);
        final Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertEquals(2, combat.getAttackers().size());
        for (final Card a : combat.getAttackers()) {
            AssertJUnit.assertSame(opp, combat.getDefenderByAttacker(a));
        }
    }

    // Soul Parry was cast in main 1 on a creature that might block, and then the AI didn't attack
    @Test
    public void testSoulParryWaitsForABlockerThatKills() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        final Card bears = ready("Grizzly Bears", ai);
        final Card giant = ready("Hill Giant", opp);
        final SpellAbility parry = addCardToZone("Soul Parry", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, parry));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, parry));

        final Combat combat = blocked(game, ai, List.of(bears), opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, parry));
        AssertJUnit.assertTrue(parry.getTargets().contains(giant));
    }

    // Roterothopter pumped itself in combat without attacking
    @Test
    public void testRoterothopterPumpsOnlyWhileAttacking() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        lands(ai, "Wastes", 4);
        final Card thopter = ready("Roterothopter", ai);
        final SpellAbility pump = thopter.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.Pump).findFirst().orElseThrow();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNotSame("not before it attacks", AiPlayDecision.WillPlay, decide(ai, pump));

        blocked(game, ai, List.of(thopter), opp);
        // pumping an unblocked attacker is a chance roll, not a certainty
        boolean pumped = false;
        for (int i = 0; i < 30 && !pumped; i++) {
            pumped = decide(ai, pump) == AiPlayDecision.WillPlay;
        }
        AssertJUnit.assertTrue("unblocked, it pumps", pumped);
    }

    @Test
    public void testRoterothopterStillAttacksWithManaUp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        lands(ai, "Wastes", 4);
        final Card thopter = ready("Roterothopter", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai, false);
        game.getAction().checkStateEffects(true);
        final Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertTrue("an open board and two pumps", combat.isAttacking(thopter));
    }
}
