package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityIgnoreErrata;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Avacyn Restored script review and R&D's Secret Lair printed text (2026-10-23). */
public class AvacynRestoredAuditTest extends AITest {

    /** Returns whether the uncastable Bear on top of the library ended up on the bottom. */
    private boolean pathPutsDeclinedCreatureOnBottom(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Island", opp, ZoneType.Library);
        }
        Card top = addCardToZone("Grizzly Bears", p, ZoneType.Library);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card path = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Descendants' Path", "AVR"), p);
        p.getZone(ZoneType.Battlefield).add(path);
        addCard("Grizzly Bears", p);
        // can't cast spells from libraries, so the Bear is revealed but never cast
        addCard("Grafdigger's Cage", p);
        p.getZone(ZoneType.Library).reorder(top, 0);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, opp);
        playUntilPhase(game, PhaseType.DRAW);
        final java.util.List<Card> library = new java.util.ArrayList<>(p.getCardsIn(ZoneType.Library));
        return library.get(library.size() - 1).getName().equals("Grizzly Bears");
    }

    @Test
    public void testDescendantsPathBottomsADeclinedCreature() {
        AssertJUnit.assertTrue(pathPutsDeclinedCreatureOnBottom(false));
    }

    @Test
    public void testPrintedDescendantsPathLeavesADeclinedCreatureOnTop() {
        AssertJUnit.assertFalse(pathPutsDeclinedCreatureOnBottom(true));
    }

    @Test
    public void testSecretLairPrintingsUseTheirOwnPrintDate() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card zen = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Lotus Cobra", "ZEN"), p);
        Card sld = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Lotus Cobra", "SLD"), p);
        AssertJUnit.assertTrue(StaticAbilityIgnoreErrata.printedBefore(zen, "20200925"));
        AssertJUnit.assertFalse(StaticAbilityIgnoreErrata.printedBefore(sld, "20200925"));
    }
}
