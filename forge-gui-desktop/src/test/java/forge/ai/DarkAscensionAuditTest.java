package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/** Dark Ascension script review (2026-10-23). */
public class DarkAscensionAuditTest extends AITest {

    @Test
    public void testElbrusUnattachesThenTransforms() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card bears = addCard("Grizzly Bears", p);
        Card elbrus = addCard("Elbrus, the Binding Blade", p);
        elbrus.attachToEntity(bears, null);
        AssertJUnit.assertTrue(bears.isEquipped());

        for (Trigger t : elbrus.getTriggers()) {
            if (t.getMode() == TriggerType.DamageDone) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(p);
                AbilityUtils.resolve(sa);
                break;
            }
        }
        game.getAction().checkStateEffects(true);
        Card withengar = game.getCardState(elbrus);
        AssertJUnit.assertEquals("Withengar Unbound", withengar.getName());
        AssertJUnit.assertFalse(bears.isEquipped());
        AssertJUnit.assertFalse(withengar.isAttachedToEntity());
    }
}
