package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.NameLetters;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Stet, Draconic Proofreader (UND #57), checked against Scryfall and its 2 rulings 2026-10-20.
 */
public class StetTest extends AITest {

    @Test
    public void letters() {
        AssertJUnit.assertEquals("rizzly Bears", NameLetters.deleteFirstLetter("Grizzly Bears"));
        AssertJUnit.assertEquals(".O.B.", NameLetters.deleteFirstLetter("B.O.B."));
        AssertJUnit.assertEquals("", NameLetters.deleteFirstLetter("X"));
        AssertJUnit.assertTrue(NameLetters.sameInitial("Giant Growth", "grizzly Bears"));
        AssertJUnit.assertFalse("nameless begins with no letter", NameLetters.sameInitial("", ""));
        AssertJUnit.assertFalse(NameLetters.sameInitial("_____", "_____"));
    }

    @Test
    public void deleteUntilEndOfTurn() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card stet = addCard("Stet, Draconic Proofreader", ai);
        Card bears = addCard("Grizzly Bears", opp);
        addCards("Plains", 2, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility delete = stet.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.DeleteFirstLetter).findFirst().orElseThrow();
        delete.setActivatingPlayer(ai);
        delete.getTargets().add(bears);
        AbilityUtils.resolve(delete);
        AssertJUnit.assertEquals("rizzly Bears", bears.getName());

        final String oppName = opp.getName();
        delete.resetTargets();
        delete.getTargets().add(opp);
        AbilityUtils.resolve(delete);
        AbilityUtils.resolve(delete);
        AssertJUnit.assertEquals(NameLetters.deleteFirstLetter(NameLetters.deleteFirstLetter(oppName)), opp.getName());

        game.getEndOfTurn().executeUntil();
        AssertJUnit.assertEquals("Grizzly Bears", bears.getName());
        AssertJUnit.assertEquals(oppName, opp.getName());
    }

    @Test
    public void attackBurnsWhatSharesTheLetter() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card stet = addCard("Stet, Draconic Proofreader", ai);
        Card bears = addCard("Grizzly Bears", opp);
        Card hill = addCard("Hill Giant", opp);
        addCardToZone("Lightning Bolt", ai, ZoneType.Graveyard);
        Card growth = addCardToZone("Giant Growth", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        Trigger attack = stet.getTriggers().stream().filter(t -> t.getMode() == TriggerType.Attacks).findFirst().orElseThrow();
        SpellAbility sa = attack.ensureAbility().copy(stet, ai, false);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue("the G card was exiled", game.getCardState(growth).isInZone(ZoneType.Exile));
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertEquals(1, game.getStack().size());
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(hill.isInPlay());
        AssertJUnit.assertFalse(stet.hasRemembered());
    }
}
