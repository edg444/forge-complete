package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Black Lotus Unknown Planechase, Fallout and Outlaws of Thunder Junction set review (2026-10-24). */
public class ThunderJunctionAuditTest extends AITest {

    private String activeDrakeCopy(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Thunderclap Drake", "OTC"), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        String found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.hasParam("Execute") && sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one version should be usable", found);
                found = sa.getParam("Execute");
            }
        }
        return found;
    }

    @Test
    public void testThunderclapDrakePrintedCopiesKeepTargets() {
        AssertJUnit.assertEquals("EffTrigCopy", activeDrakeCopy(false));
        AssertJUnit.assertEquals("EffTrigCopyPrinted", activeDrakeCopy(true));
    }

    @Test
    public void testVaultElevenChapterOneText() {
        Game game = initAndCreateGame();
        String t = addCardToZone("Vault 11: Voter's Dilemma", game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
        AssertJUnit.assertTrue(t, t.contains("I — For each opponent, you create a 1/1 white Human Soldier creature token."));
    }
}
