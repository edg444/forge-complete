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

/** Ixalan R&D's Secret Lair printed text (2026-10-23). */
public class IxalanAuditTest extends AITest {

    private SpellAbility activeTrigger(final String name, final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
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
    public void testSearchForAzcantaPrintedTextIsntASurveil() {
        AssertJUnit.assertEquals(ApiType.Surveil, activeTrigger("Search for Azcanta", "XLN", false).getApi());
        SpellAbility printed = activeTrigger("Search for Azcanta", "XLN", true);
        AssertJUnit.assertEquals(ApiType.Dig, printed.getApi());
        AssertJUnit.assertEquals(ApiType.SetState, printed.getSubAbility().getApi());
        AssertJUnit.assertEquals(ApiType.Surveil, activeTrigger("Search for Azcanta", "SLD", true).getApi());
    }

    @Test
    public void testHostageTakerPrintedTextCanTargetItself() {
        String oracle = String.join(",", activeTrigger("Hostage Taker", "XLN", false).getTargetRestrictions().getValidTgts());
        AssertJUnit.assertEquals("Creature.Other,Artifact.Other", oracle);
        String printed = String.join(",", activeTrigger("Hostage Taker", "XLN", true).getTargetRestrictions().getValidTgts());
        AssertJUnit.assertEquals("Artifact,Creature", printed);
        String afc = String.join(",", activeTrigger("Hostage Taker", "AFC", true).getTargetRestrictions().getValidTgts());
        AssertJUnit.assertEquals("Creature.Other,Artifact.Other", afc);
    }
}
