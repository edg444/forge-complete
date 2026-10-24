package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Warhammer 40,000 Commander and Unfinity set review (2026-10-24). */
public class Warhammer40kUnfinityAuditTest extends AITest {

    private List<String> activeTriggers(final String name, final String set, final String ability, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        List<String> found = new ArrayList<>();
        for (Trigger t : c.getTriggers()) {
            if (t.getParamOrDefault("TriggerDescription", "").contains(ability) && t.requirementsCheck(game)) {
                found.add(t.getParam("TriggerDescription"));
            }
        }
        return found;
    }

    private void assertSwitch(final String name, final String set, final String ability) {
        List<String> oracle = activeTriggers(name, set, ability, false);
        AssertJUnit.assertEquals(oracle.toString(), 1, oracle.size());
        AssertJUnit.assertFalse(oracle.get(0), oracle.get(0).startsWith("(As printed)"));
        List<String> printed = activeTriggers(name, set, ability, true);
        AssertJUnit.assertEquals(printed.toString(), 1, printed.size());
        AssertJUnit.assertTrue(printed.get(0), printed.get(0).startsWith("(As printed)"));
    }

    @Test
    public void testLairVersions() {
        assertSwitch("Triumph of Saint Katherine", "40K", "Praesidium Protectiva");
        assertSwitch("Canoptek Wraith", "40K", "Transdimensional Scout");
        assertSwitch("Captain Rex Nebula", "UNF", "At the beginning of combat");
    }

    private String text(final String name) {
        Game game = initAndCreateGame();
        return addCardToZone(name, game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
    }

    @Test
    public void testPrizeKeywordIsNotPrintedRaw() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Cover the Spot"), p);
        String t = c.getAbilityText();
        AssertJUnit.assertFalse(t, t.contains("Prize:"));
        AssertJUnit.assertTrue(t, t.contains("Prize — You get"));
    }

    @Test
    public void testTapCostDescriptionPlacesYouControl() {
        String t = text("T.A.P.P.E.R.");
        AssertJUnit.assertTrue(t, t.contains("Tap an untapped creature you control that has a hat: Tap target creature."));
    }

    @Test
    public void testMacieKeepsHerLeftTappedClause() {
        String t = text("Grand Marshal Macie");
        AssertJUnit.assertTrue(t, t.contains("during your untap step. If you do, put a pause counter on it"));
        AssertJUnit.assertTrue(t, t.contains("Whenever Macie becomes untapped"));
    }

    @Test
    public void testPermanentSpellCostShowsItsAbilityWord() {
        String t = text("Hierophant Bio-Titan");
        AssertJUnit.assertTrue(t, t.contains("Frenzied Metabolism — As an additional cost to cast this spell, you may remove"));
    }
}
