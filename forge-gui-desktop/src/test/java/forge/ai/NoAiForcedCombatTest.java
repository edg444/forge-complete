package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.ForcedCombatAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** ForcedCombatAi: AILogic$ ForceAttack, ForceBlock, Lure and MustBeBlocked. */
public class NoAiForcedCombatTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            s.resetTargets();
        }
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c, int n) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility() && n-- == 0) {
                return sa;
            }
        }
        return null;
    }

    private Card ready(String name, Player p) {
        Card c = addCard(name, p);
        c.setSickness(false);
        return c;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Alluring Siren: "{T}: Target creature an opponent controls attacks you this turn if able." - into a blocker
    // that kills it and lives
    @Test
    public void testAlluringSirenPullsIntoAGoodBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card siren = ready("Alluring Siren", ai);
        Card bears = ready("Grizzly Bears", opp);
        SpellAbility sa = activated(siren, 0);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, opp);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        ready("Serra Angel", ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Nettling Imp: a creature that can't attack is destroyed at end of turn
    @Test
    public void testNettlingImpKillsATappedCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card imp = ready("Nettling Imp", ai);
        Card giant = ready("Hill Giant", opp);
        giant.setTapped(true);
        SpellAbility sa = activated(imp, 0);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, opp);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
    }

    // Siren's Call: every creature the active player controls; their tapped ones die, but not if the rest is lethal
    @Test
    public void testSirensCallPunishesTappedCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 1);
        ready("Hill Giant", opp).setTapped(true);
        ready("Grizzly Bears", opp).setTapped(true);
        SpellAbility sa = addCardToZone("Siren's Call", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, opp);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        ready("Craw Wurm", opp);
        ai.setLife(6, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Walking Desecration: the creature type is the one it decided on
    @Test
    public void testWalkingDesecrationNamesTheirType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 1);
        Card desecration = ready("Walking Desecration", ai);
        ready("Serra Angel", ai);
        ready("Grizzly Bears", opp);
        SpellAbility sa = activated(desecration, 0);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, opp);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Bear", ForcedCombatAi.chosenType(desecration));
    }

    // Culling Mark: "Target creature blocks this turn if able." - one the attack kills
    @Test
    public void testCullingMarkOnABlockerTheAttackKills() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 3);
        ready("Hill Giant", ai);
        Card wurm = ready("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Culling Mark", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        wurm.setTapped(true);
        Card bears = ready("Grizzly Bears", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Provoke: "Untap target creature you don't control. That creature blocks this turn if able. Draw a card."
    @Test
    public void testProvokeUntapsABlockerIntoTheAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 2);
        Card giant = ready("Hill Giant", ai);
        Card bears = ready("Grizzly Bears", opp);
        bears.setTapped(true);
        SpellAbility sa = addCardToZone("Provoke", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(giant, opp);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Mark for Death: the one creature that may block, so the rest of the attack is lethal
    @Test
    public void testMarkForDeathLetsTheAttackThrough() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 4);
        for (int i = 0; i < 3; i++) {
            ready("Hill Giant", ai);
        }
        for (int i = 0; i < 3; i++) {
            ready("Wall of Stone", opp);
        }
        opp.setLife(6, null);
        SpellAbility sa = addCardToZone("Mark for Death", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Bloodscent: every blocker goes to one attacker, and the rest gets through for lethal
    @Test
    public void testBloodscentForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 4);
        ready("Hill Giant", ai);
        ready("Hill Giant", ai);
        ready("Serra Angel", ai);
        ready("Grizzly Bears", opp);
        ready("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Bloodscent", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        opp.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(6, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // ...and the attack AI counts the blockers the lure takes, so it goes in with everything
    @Test
    public void testAttackAfterALure() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 4);
        ready("Hill Giant", ai);
        ready("Hill Giant", ai);
        ready("Serra Angel", ai);
        ready("Craw Wurm", opp);
        ready("Craw Wurm", opp);
        opp.setLife(6, null);
        SpellAbility sa = addCardToZone("Bloodscent", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        Combat before = new Combat(ai);
        new AiAttackController(ai).declareAttackers(before);
        AssertJUnit.assertTrue(before.getAttackers().size() < 3);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        forge.game.ability.AbilityUtils.resolve(sa);
        Combat after = new Combat(ai);
        new AiAttackController(ai).declareAttackers(after);
        AssertJUnit.assertEquals(3, after.getAttackers().size());
    }

    // Irresistible Prey: "Target creature must be blocked this turn if able. Draw a card."
    @Test
    public void testIrresistiblePreyOnAnAttackerNothingSurvives() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 1);
        Card wurm = ready("Craw Wurm", ai);
        ready("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Irresistible Prey", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }
}
