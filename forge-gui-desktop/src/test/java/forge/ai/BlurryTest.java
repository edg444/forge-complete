package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Blurry Beeble's Blurry: only a defending player who was wearing glasses as it was cast can block it, and one
 * put onto the battlefield without being cast can't be blocked at all (Unstable rulings).
 */
public class BlurryTest extends AITest {

    private static SpellAbility glassesQuestion(Card beeble) {
        for (Trigger t : beeble.getTriggers()) {
            if (t.getMode() == TriggerType.SpellCast) {
                return t.ensureAbility();
            }
        }
        return null;
    }

    @Test
    public void testEachPlayersAnswerIsRemembered() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card beeble = addCardToZone("Blurry Beeble", me, ZoneType.Hand);

        SpellAbility sa = glassesQuestion(beeble);
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);

        // the AI's answer is its per-game glasses roll, and every player is asked, the caster included
        for (Player p : game.getPlayers()) {
            AssertJUnit.assertEquals(p.getName(),
                    SpellAbilityAi.rollGameChanceLogic(p, sa, "GameChance.50.Glasses"), beeble.isRemembered(p));
        }
    }

    @Test
    public void testOnlyGlassesWearersCanBlock() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card beeble = addCard("Blurry Beeble", me);
        beeble.setCastFrom(me.getZone(ZoneType.Hand));
        Card bears = addCard("Grizzly Bears", opp);

        AssertJUnit.assertFalse(CombatUtil.canBlock(beeble, bears));
        beeble.addRemembered(me);
        AssertJUnit.assertFalse(CombatUtil.canBlock(beeble, bears));
        beeble.addRemembered(opp);
        AssertJUnit.assertTrue(CombatUtil.canBlock(beeble, bears));
    }

    @Test
    public void testUncastBeebleCantBeBlocked() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card beeble = addCard("Blurry Beeble", me);
        Card bears = addCard("Grizzly Bears", opp);

        // e.g. flickered: the copy made on leaving the battlefield keeps its remembered players
        beeble.addRemembered(opp);
        AssertJUnit.assertFalse(CombatUtil.canBlock(beeble, bears));
    }
}
