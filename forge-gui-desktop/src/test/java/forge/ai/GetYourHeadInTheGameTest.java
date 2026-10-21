package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Get Your Head in the Game (UNF #10), checked against Scryfall and its 4 rulings 2026-10-20. Touching your head is
 * physical: a human reports when contact breaks (honor system); the AI's grip slips with a 5% chance at each of its
 * upkeeps (user ruling).
 */
public class GetYourHeadInTheGameTest extends AITest {

    private SpellAbility stoppedTouching(final Card c, final Player p) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.hasParam("SpecialAction")) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError("no honor-system action");
    }

    @Test
    public void testExileUntilItLeavesAndTheHonorAction() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card bears = addCard("Grizzly Bears", opp);
        final Card head = addCard("Get Your Head in the Game", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("not touching your head before the trigger", stoppedTouching(head, p).canPlay());

        final SpellAbility etb = head.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(p);
        etb.getTargets().add(bears);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals("Number$1", head.getSVar("OnHead"));
        AssertJUnit.assertTrue(stoppedTouching(head, p).canPlay());

        AbilityUtils.resolve(stoppedTouching(head, p));
        AssertJUnit.assertEquals("the action can't be repeated", "Number$0", head.getSVar("OnHead"));
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
        AssertJUnit.assertTrue(game.getCardState(head).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("the exiled permanent comes back", game.getCardState(bears).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testTheAiNeverReportsAHumansHonorAction() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card head = addCard("Get Your Head in the Game", ai);
        head.setSVar("OnHead", "Number$1");
        final SpellAbility action = stoppedTouching(head, ai);
        AssertJUnit.assertTrue(action.canPlay());
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(action) == AiPlayDecision.WillPlay);

        final Card form = addCard("Form of the Approach of the Second Sun", ai);
        for (final SpellAbility sa : form.getSpellAbilities()) {
            if (sa.hasParam("SpecialAction")) {
                sa.setActivatingPlayer(ai);
                AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay);
            }
        }
    }

    @Test
    public void testIsAIPlayerProperty() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card head = addCard("Get Your Head in the Game", ai);
        AssertJUnit.assertTrue(ai.isValid("You.IsAI", ai, head, null));
    }
}
