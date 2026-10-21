package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Far Out (UNF #8), checked against Scryfall and its 5 rulings 2026-10-20: one or more modes rather than the indicated
 * number, never the same mode twice, modes still need legal targets, and a random modal spell stays random.
 */
public class FarOutTest extends AITest {

    // each mode of these cards has its own API; a chosen mode is a clone that still carries the rest of the chain
    private static List<String> chosenModes(final SpellAbility sa) {
        final List<String> modes = new ArrayList<>();
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (!modes.contains(String.valueOf(sub.getApi()))) {
                modes.add(String.valueOf(sub.getApi()));
            }
        }
        return modes;
    }

    private SpellAbility kolaghansCommand(final Game game, final Player ai, final Player opp) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCard("Ornithopter", opp);
        final Card command = addCardToZone("Kolaghan's Command", ai, ZoneType.Hand);
        final SpellAbility sa = command.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void testWithoutFarOutTheIndicatedNumber() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = kolaghansCommand(game, ai, game.getPlayers().get(0));
        CharmEffect.makeChoices(sa);
        AssertJUnit.assertTrue(chosenModes(sa).size() <= 2);
    }

    @Test
    public void testFarOutLetsTheAiChooseMoreThanTheIndicatedNumber() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Far Out", ai);
        final SpellAbility sa = kolaghansCommand(game, ai, game.getPlayers().get(0));
        CharmEffect.makeChoices(sa);
        final List<String> modes = chosenModes(sa);
        AssertJUnit.assertTrue(modes.toString(), modes.size() > 2);
    }

    @Test
    public void testWhenFarOutAppliesAndWhenItChangesNothing() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card confluence = addCardToZone("Fiery Confluence", ai, ZoneType.Hand);
        final SpellAbility sa = confluence.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("no Far Out", CharmEffect.canChooseAnyNumberOfModes(sa, ai, 3, 3, 3, true));
        addCard("Far Out", ai);
        // choose three, repeats allowed: one or two different modes, or three without repeating, become possible
        AssertJUnit.assertTrue(CharmEffect.canChooseAnyNumberOfModes(sa, ai, 3, 3, 3, true));
        // choose two of four
        AssertJUnit.assertTrue(CharmEffect.canChooseAnyNumberOfModes(sa, ai, 4, 2, 2, false));
        // already "choose one or more": nothing to change, so nothing is asked
        AssertJUnit.assertFalse(CharmEffect.canChooseAnyNumberOfModes(sa, ai, 3, 1, 3, false));
        // an opponent choosing the modes of your spell isn't you choosing
        AssertJUnit.assertFalse(CharmEffect.canChooseAnyNumberOfModes(sa, opp, 4, 2, 2, false));
    }

    @Test
    public void testOnlyItsControllersModes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Far Out", opp);
        final SpellAbility sa = kolaghansCommand(game, ai, opp);
        CharmEffect.makeChoices(sa);
        AssertJUnit.assertTrue("an opponent's Far Out does nothing", chosenModes(sa).size() <= 2);
    }
}
