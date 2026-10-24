package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 87. */
public class NoAiBatch87Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Dance of the Skywise: its Grizzly Bears becomes a 4/4 flying Dragon before attacking
    @Test
    public void testDanceOfTheSkywiseUpgradesTheBears() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Island", ai);
        addCard("Island", ai);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        SpellAbility sa = addCardToZone("Dance of the Skywise", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Hatred: pays exactly the life the unblocked attacker needs for lethal
    @Test
    public void testHatredPaysForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Swamp", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        opp.setLife(7, null);
        SpellAbility sa = addCardToZone("Hatred", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        forge.game.combat.Combat combat = new forge.game.combat.Combat(ai);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(5, sa.getXManaCostPaid().intValue());
    }

    // Inner Struggle: their Craw Wurm's 6 power kills it
    @Test
    public void testInnerStruggleOnThePowerful() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        Card wurm = addCard("Craw Wurm", opp);
        addCard("Wall of Stone", opp);
        SpellAbility sa = addCardToZone("Inner Struggle", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Demonic Collusion: a tutor the AI already knew how to use
    @Test
    public void testDemonicCollusionTutors() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Swamp", ai);
        }
        for (int i = 0; i < 5; i++) {
            addCardToZone("Serra Angel", ai, ZoneType.Library);
        }
        SpellAbility sa = addCardToZone("Demonic Collusion", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
