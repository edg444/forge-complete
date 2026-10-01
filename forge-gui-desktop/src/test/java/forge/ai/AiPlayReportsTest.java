package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** AI misplays reported from play on 2026-09-30, each set up the way it happened. */
public class AiPlayReportsTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Vibranium Strike Gauntlets: "Flash / When this Equipment enters, attach it to target creature you control. ..."
    @Test
    public void testGauntletsNeedACreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Wastes", 5);
        SpellAbility sa = addCardToZone("Vibranium Strike Gauntlets", ai, ZoneType.Hand).getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    @Test
    public void testGauntletsNotFlashedOntoACreatureAboutToDie() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Wastes", 5);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Vibranium Strike Gauntlets", ai, ZoneType.Hand).getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        // a 2/2 blocked by a 3/3 dies with or without +3/+0
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    @Test
    public void testGauntletsGoOnTheCreatureThatSurvives() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        Card gauntlets = addCard("Vibranium Strike Gauntlets", ai);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        Combat combat = new Combat(ai);
        combat.addAttacker(giant, opp);
        combat.addAttacker(bears, opp);
        combat.addBlocker(giant, wurm);
        combat.setBlocked(giant, true);
        combat.setBlocked(bears, false);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);

        SpellAbility attach = null;
        for (Trigger t : gauntlets.getTriggers()) {
            if (t.ensureAbility() != null && t.ensureAbility().getApi() == ApiType.Attach) {
                attach = t.ensureAbility().copy(ai);
            }
        }
        attach.resetTargets();
        SpellApiToAi.Converter.get(attach).doTriggerNoCostWithSubs(ai, attach, true);
        // the Hill Giant is the better creature, but the Craw Wurm is about to kill it
        AssertJUnit.assertEquals(bears, attach.getTargetCard());
    }

    // Terror of Mount Velus: "When this creature enters, creatures you control gain double strike until end of turn."
    @Test
    public void testDoubleStrikeForEveryoneComesBeforeCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = addCardToZone("Terror of Mount Velus", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(ComputerUtil.castPermanentInMain1(ai, sa));
        addCard("Grizzly Bears", ai).setSickness(false);
        AssertJUnit.assertTrue(ComputerUtil.castPermanentInMain1(ai, sa));
    }

    // Signal Pest: "Battle cry ... This creature can't be blocked except by creatures with flying or reach." 0/1.
    @Test
    public void testSignalPestDoesNotAttackAlone() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card pest = addCard("Signal Pest", ai);
        pest.setSickness(false);
        addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertTrue(combat.getAttackers().isEmpty());

        // with someone to boost, it goes
        Card ogre = addCard("Gray Ogre", ai);
        ogre.setSickness(false);
        opp.getCreaturesInPlay().forEach(c -> game.getAction().moveToGraveyard(c, null));
        game.getAction().checkStateEffects(true);
        combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertTrue(combat.isAttacking(ogre));
        AssertJUnit.assertTrue(combat.isAttacking(pest));
    }

    // Maro: "Maro's power and toughness are each equal to the number of cards in your hand."
    @Test
    public void testHandSizeCreatureKeepsTheHand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 3);
        lands(ai, "Mountain", 3);
        addCard("Grizzly Bears", ai).setSickness(false);
        addCard("Hill Giant", opp);
        SpellAbility aura = addCardToZone("Holy Strength", ai, ZoneType.Hand).getFirstSpellAbility();
        SpellAbility bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, aura));

        addCard("Maro", ai).setSickness(false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, HandSizeAi.pointsPerCardInHand(ai));
        // +1/+2 on a creature isn't worth -1/-1 on Maro; removal is
        AssertJUnit.assertEquals(AiPlayDecision.KeepInHandForSize, decide(ai, aura));
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, bolt));

        // over the maximum hand size the card would be discarded anyway
        for (int i = 0; i < 7; i++) {
            addCardToZone("Island", ai, ZoneType.Hand);
        }
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, aura));
    }
}
