package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.cost.CostReveal;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Unstable and Rivals of Ixalan review (2026-10-23). */
public class UnstableRivalsAuditTest extends AITest {

    private SpellAbility ponyTrigger(final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Ordinary Pony", set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        SpellAbility found = null;
        for (Trigger t : c.getTriggers()) {
            if (t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = t.ensureAbility();
            }
        }
        return found;
    }

    @Test
    public void testOrdinaryPonyPrintedTextCanReflickerTheSameCreature() {
        String oracle = ponyTrigger("UST", false).getTargetRestrictions().getValidTgts()[0];
        AssertJUnit.assertTrue(oracle.contains("!hasKeywordReturnedByOrdinaryPony"));
        AssertJUnit.assertEquals("Creature.nonHorse+YouCtrl", ponyTrigger("UST", true).getTargetRestrictions().getValidTgts()[0]);
        AssertJUnit.assertEquals(oracle, ponyTrigger("UND", true).getTargetRestrictions().getValidTgts()[0]);
    }

    @Test
    public void testTetzimocRevealsItselfToMarkPrey() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCardToZone("Tetzimoc, Primal Death", p, ZoneType.Hand);
        boolean reveals = false;
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                for (CostPart part : sa.getPayCosts().getCostParts()) {
                    reveals |= part instanceof CostReveal && part.payCostFromSource();
                }
            }
        }
        AssertJUnit.assertTrue(reveals);
    }
}
