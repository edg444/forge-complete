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

/** Doctor Who and The Lost Caverns of Ixalan set review (2026-10-24). */
public class DoctorWhoAuditTest extends AITest {

    private List<String> oodTriggers(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card plane = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Ood Sphere"), p);
        p.getZone(ZoneType.Command).add(plane);
        game.getAction().checkStateEffects(true);
        List<String> found = new ArrayList<>();
        for (Trigger t : plane.getTriggers()) {
            if (t.requirementsCheck(game)) {
                found.add(t.getParam("TriggerDescription"));
            }
        }
        return found;
    }

    @Test
    public void testOodSphereErrataAndPrintedVersion() {
        List<String> oracle = oodTriggers(false);
        AssertJUnit.assertEquals(oracle.toString(), 1, oracle.size());
        AssertJUnit.assertTrue(oracle.get(0), oracle.get(0).contains("Until your next turn"));
        List<String> printed = oodTriggers(true);
        AssertJUnit.assertEquals(printed.toString(), 1, printed.size());
        AssertJUnit.assertTrue(printed.get(0), printed.get(0).startsWith("(As printed)"));
    }

    @Test
    public void testWaytaShowsItsCostReduction() {
        Game game = initAndCreateGame();
        String t = addCardToZone("Wayta, Trainer Prodigy", game.getPlayers().get(1), ZoneType.Hand).getAbilityText();
        AssertJUnit.assertTrue(t, t.contains("This ability costs {2} less to activate if it targets two creatures you control."));
    }
}
