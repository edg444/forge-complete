package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class CombatRemovalAndEquipAiTest extends AITest {

    @Test
    public void testStrengthTestingHammerRollCountsAtLeastOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card cadet = addCard("Eager Cadet", ai);
        Card hammer = addCard("Strength-Testing Hammer", ai);
        hammer.attachToEntity(cadet, null);
        Card bareCadet = addCard("Eager Cadet", ai);
        Card blocker = addCard("Norwood Ranger", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        Combat combat = new Combat(ai);

        AssertJUnit.assertEquals(1, ComputerUtilCombat.predictPowerBonusOfAttacker(cadet, blocker, combat, false));
        AssertJUnit.assertTrue("a Hammer-equipped 1/1 always kills a 1/2 blocker",
                ComputerUtilCombat.canDestroyBlocker(opp, blocker, cadet, combat, false));
        AssertJUnit.assertEquals(0, ComputerUtilCombat.predictPowerBonusOfAttacker(bareCadet, blocker, combat, false));
        AssertJUnit.assertFalse(ComputerUtilCombat.canDestroyBlocker(opp, blocker, bareCadet, combat, false));
    }

    @Test
    public void testVelukanDragonRollMinusOnePredictsZero() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card dragon = addCard("Velukan Dragon", ai);
        Card blocker = addCard("Norwood Ranger", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(0, ComputerUtilCombat.predictPowerBonusOfAttacker(dragon, blocker, new Combat(ai), false));
    }

    @Test
    public void testWallOfNetsRemovesAttackerItSurvives() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card wall = addCard("Wall of Nets", opp);
        Card fiveFive = addCard("Colossapede", ai);
        Card eightEight = addCard("Colossapede", ai);
        eightEight.setCounters(CounterEnumType.P1P1, 3);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        Combat combat = new Combat(ai);

        AssertJUnit.assertTrue("a 5/5 blocked by the 0/7 wall is exiled at end of combat",
                ComputerUtilCombat.canDestroyAttacker(opp, fiveFive, wall, combat, false));
        AssertJUnit.assertFalse("an 8/8 kills the wall before its end-of-combat trigger",
                ComputerUtilCombat.canDestroyAttacker(opp, eightEight, wall, combat, false));
    }

    @Test
    public void testAiDoesNotAttackIntoWallOfNets() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Wall of Nets", opp);
        Card fiveFive = addCard("Colossapede", ai);
        Card bear = addCard("Grizzly Bears", ai);
        fiveFive.setSickness(false);
        bear.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Zero defending power used to read as a free attack, so both went in and the wall exiled the 5/5.
        Combat combat = new Combat(ai);
        new AiAttackController(ai).declareAttackers(combat);
        AssertJUnit.assertTrue("whichever attacker the wall blocks is exiled", combat.getAttackers().isEmpty());
    }

    @Test
    public void testKjeldoranFrostbeastDestroysItsBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card frostbeast = addCard("Kjeldoran Frostbeast", ai);
        Card wall = addCard("Wall of Wood", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("the 0/3 survives the 2 damage but not the end-of-combat destroy",
                ComputerUtilCombat.canDestroyBlocker(opp, wall, frostbeast, new Combat(ai), false));
    }

    @Test
    public void testSecondWitchesEyeGoesToAnotherCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card bear = addCard("Grizzly Bears", ai);
        Card fiveFive = addCard("Colossapede", ai);
        addCard("Witches' Eye", ai).attachToEntity(bear, null);
        SpellAbility equip = equipAbility(addCard("Witches' Eye", ai), ai);
        addCards("Forest", 3, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(equip).canPlayWithSubs(ai, equip).willingToPlay());
        AssertJUnit.assertEquals("a second scry-tap ability on the same creature adds nothing",
                fiveFive, equip.getTargetCard());
    }

    @Test
    public void testSecondWitchesEyeNotEquippedToOnlyCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card only = addCard("Colossapede", ai);
        addCard("Witches' Eye", ai).attachToEntity(only, null);
        SpellAbility equip = equipAbility(addCard("Witches' Eye", ai), ai);
        addCards("Forest", 3, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(equip).canPlayWithSubs(ai, equip).willingToPlay());
    }

    private static SpellAbility equipAbility(Card equipment, Player ai) {
        for (SpellAbility sa : equipment.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Attach) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError(equipment + " has no equip ability");
    }

}
