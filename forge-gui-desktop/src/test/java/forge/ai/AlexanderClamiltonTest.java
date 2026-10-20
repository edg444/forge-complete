package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Alexander Clamilton (UND #16), checked against Scryfall and its 6 rulings 2026-10-20: reminder text and flavor text
 * aren't rules text, so a line that is only reminder text isn't counted (CR 207.2 agrees).
 */
public class AlexanderClamiltonTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner) {
        return Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
    }

    private static Card onTop(final String name, final String set, final Player p) {
        Card c = printing(name, set, p);
        p.getZone(ZoneType.Library).add(c, 0);
        return c;
    }

    @Test
    public void reminderOnlyLinesArentRulesText() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        // Deathtouch and Lifelink, each followed by a line that is only reminder text
        AssertJUnit.assertEquals(2, printing("Vampire of the Dire Moon", "M20", ai).getPrintedRulesLines());
        AssertJUnit.assertEquals("Sagas aren't read", -1, printing("History of Benalia", "DOM", ai).getPrintedRulesLines());
    }

    private Card fight(final Game game, final String top, final String set) {
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 2, ai);
        Card clam = addCard("Alexander Clamilton", ai);
        Card bears = addCard("Grizzly Bears", opp);
        fillLibrary(ai, 3);
        onTop(top, set, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = clam.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.PeekAndReveal).findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertSame(bears, sa.getTargetCard());
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(2, clam.getDamage());
        return clam;
    }

    @Test
    public void readLinesPumpAndFight() {
        Game game = initAndCreateGame();
        Card clam = fight(game, "Vampire of the Dire Moon", "M20");
        AssertJUnit.assertEquals(2, clam.getNetPower());
    }

    @Test
    public void unreadCardIsCountedByItsController() {
        Game game = initAndCreateGame();
        // the AI counts one line per Oracle paragraph with rules text: the Saga reminder paragraph doesn't count
        Card clam = fight(game, "History of Benalia", "DOM");
        AssertJUnit.assertEquals(2, clam.getNetPower());
    }

    @Test
    public void wordySpellScries() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Alexander Clamilton", ai);
        addCards("Island", 4, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        // five printed lines, no reminder text
        Card factOrFiction = printing("Fact or Fiction", "INV", ai);
        ai.getZone(ZoneType.Hand).add(factOrFiction);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(factOrFiction.isWordy());
        SpellAbility spell = factOrFiction.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, spell, null));
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertEquals("Clamilton's scry trigger is on the stack", 2, game.getStack().size());

        // one line on this printing
        AssertJUnit.assertFalse(printing("Harmonize", "PLC", ai).isWordy());
    }
}
