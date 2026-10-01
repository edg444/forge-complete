package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * "Target creature an opponent controls doesn't untap during its controller's next untap step" belongs on a
 * tapped creature. Kefnet's Monument's script didn't mark it a curse, so the AI treated it as a buff and forced
 * it onto the opponent's worst creature; and as a curse, a main-1 cast (the usual time) found no useful target
 * and fell back to their best creature, tapped or not.
 */
public class DoesntUntapCurseAiTest extends AITest {

    @Test
    public void kefnetsMonumentTargetsTheTappedCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card monument = addCard("Kefnet's Monument", ai);
        // the tapped one is neither the best (curse fallback) nor the worst (the old non-curse fallback)
        Card best = addCard("Craw Wurm", opp);
        Card tapped = addCard("Hill Giant", opp);
        tapped.setTapped(true);
        Card worst = addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        SpellAbility sa = null;
        for (Trigger t : monument.getTriggers()) {
            SpellAbility ability = t.ensureAbility();
            if (ability != null && ability.getApi() == ApiType.Pump) {
                sa = ability;
            }
        }
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, true));
        AssertJUnit.assertEquals(tapped, sa.getTargets().getFirstTargetedCard());
        AssertJUnit.assertFalse(sa.getTargets().contains(best));
        AssertJUnit.assertFalse(sa.getTargets().contains(worst));
    }
}
