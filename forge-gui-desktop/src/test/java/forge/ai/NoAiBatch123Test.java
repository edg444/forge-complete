package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.NumberGuessAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 123: Belbe's Armor, Defensive Maneuvers, Acidic Dagger, Aether Shockwave, Chimeric Staff, Ovinomancer. */
public class NoAiBatch123Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private static Combat fight(Game game, Player attacker, Card att, Player defender, Card blocker, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        if (blocker != null) {
            combat.addBlocker(att, blocker);
        }
        combat.setBlocked(att, blocker != null);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Belbe's Armor: its Serra Angel blocking a Craw Wurm takes 6, so X of 3 keeps it alive
    @Test
    public void testBelbesArmorPaysJustEnough() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = ability(addCard("Belbe's Armor", ai));
        fight(game, opp, wurm, ai, angel, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(3), sa.getXManaCostPaid());
    }

    // Defensive Maneuvers: its Bears blocking a Hill Giant live with +0/+4, so it names Bear
    @Test
    public void testDefensiveManeuversNamesTheSavedType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Defensive Maneuvers", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        fight(game, opp, giant, ai, bears, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Bear", NumberGuessAi.plannedType(sa));
    }

    // Acidic Dagger: on its attacking Bears, which their Hill Giant can block
    @Test
    public void testAcidicDaggerOnAnAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        SpellAbility sa = ability(addCard("Acidic Dagger", ai));
        fight(game, ai, bears, opp, null, PhaseType.COMBAT_DECLARE_ATTACKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Aether Shockwave: on their turn before attacks, tapping their non-Spirits stops 7 power for its 2
    @Test
    public void testAetherShockwaveTapsTheirAttackers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        addCard("Serra Angel", opp);
        Card shock = addCardToZone("Aether Shockwave", ai, ZoneType.Hand);
        SpellAbility mode = forge.game.ability.AbilityFactory.getAbility(shock.getSVar("DBTapAll2"), shock);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, mode));

        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, mode));
    }

    // Chimeric Staff: a 5/5 blocker for their attacking Hill Giant, which it kills and survives
    @Test
    public void testChimericStaffBlocks() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = ability(addCard("Chimeric Staff", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        fight(game, opp, giant, ai, null, PhaseType.COMBAT_DECLARE_ATTACKERS);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(5), sa.getXManaCostPaid());
    }

    // Ovinomancer: only with three basic lands to return
    @Test
    public void testOvinomancerNeedsThreeBasics() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Island", ai);
        addCard("Island", ai);
        addCard("Tundra", ai);
        SpellAbility sa = addCardToZone("Ovinomancer", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Island", ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
