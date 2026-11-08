package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Live game, 2026-11-07. The AI never equipped Strider Harness ({1}: +1/+1 and haste) to a creature that could already
 * attack: a bonus under +4 total counted only through its keywords, and haste helps only a summoning-sick creature.
 */
public class StriderHarnessEquipAiTest extends AITest {

    private Game game;
    private Player ai;

    private Card setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getPhaseHandler().onStackResolved();
        addCards("Plains", 2, ai);
        return addCard("Strider Harness", ai);
    }

    private SpellAbility equip(final Card harness) {
        final SpellAbility sa = harness.getSpellAbilities().stream().filter(SpellAbility::isEquip).findFirst().get();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private Card creature(final String name, final boolean sick) {
        final Card c = addCard(name, ai);
        c.setSickness(sick);
        return c;
    }

    @Test
    public void equipsACreatureThatCanAlreadyAttack() {
        final Card harness = setUp();
        final Card bears = creature("Grizzly Bears", false);
        game.getAction().checkStateEffects(true);
        final SpellAbility sa = equip(harness);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }

    @Test
    public void hasteStillGoesToTheSummoningSickCreature() {
        final Card harness = setUp();
        creature("Hill Giant", false);
        final Card bears = creature("Grizzly Bears", true);
        game.getAction().checkStateEffects(true);
        final SpellAbility sa = equip(harness);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }
}
