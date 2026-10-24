package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Dominaria United and its Commander set review (2026-10-24). */
public class DominariaUnitedAuditTest extends AITest {

    @Test
    public void testAggressiveSabotageTargetsAnyPlayer() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        SpellAbility sa = addCardToZone("Aggressive Sabotage", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.canTarget(p));
    }

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testKeywordAfterFlashAndKickerHasNoLeadingComma() {
        String t = text("Cleaving Skyrider");
        AssertJUnit.assertFalse(t, t.contains(", flying"));
        AssertJUnit.assertTrue(t, t.contains("Flying"));
    }

    @Test
    public void testConsecutiveProtectionsJoin() {
        String t = text("Greensleeves, Maro-Sorcerer");
        AssertJUnit.assertTrue(t, t.contains("Protection from planeswalkers and from Wizards"));
        t = text("Elite Inquisitor");
        AssertJUnit.assertTrue(t, t.contains("Protection from Vampires, from Werewolves, and from Zombies"));
    }

    @Test
    public void testLegendSacrificeCostKeepsItsName() {
        String t = text("Vohar, Vodalian Desecrator");
        AssertJUnit.assertTrue(t, t.contains("{2}, Sacrifice Vohar:"));
    }

    @Test
    public void testEmptyCostDescLeavesNoDoubleSpace() {
        String t = text("Yotia Declares War");
        AssertJUnit.assertTrue(t, t.contains("II — Tap any number of untapped artifacts you control. When you do"));
    }

    private ApiType uurgApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Uurg, Spawn of Turg", "DMU"), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        ApiType found = null;
        for (Trigger t : c.getTriggers()) {
            if (t.requirementsCheck(game)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = t.ensureAbility().getApi();
            }
        }
        return found;
    }

    @Test
    public void testUurgPrintedVersionLooksInsteadOfSurveilling() {
        AssertJUnit.assertEquals(ApiType.Surveil, uurgApi(false));
        AssertJUnit.assertEquals(ApiType.Dig, uurgApi(true));
    }
}
