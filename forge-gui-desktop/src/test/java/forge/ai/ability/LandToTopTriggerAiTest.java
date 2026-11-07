package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Silver Deputy's "you may search for a basic land and put it on top" replaces the next draw with a land: the AI takes
 * it only while short on lands, not every time (user report, 2026-11-06).
 */
public class LandToTopTriggerAiTest extends AITest {

    @Test
    public void searchesWhenShortOnLands() {
        AssertJUnit.assertTrue("should put a land on top with 2 lands in play and none in hand", searches(setup(2, 0)));
    }

    @Test
    public void declinesWithLandInHand() {
        AssertJUnit.assertFalse("should not replace its draw with a land while holding lands", searches(setup(3, 2)));
    }

    @Test
    public void declinesWhenFlooded() {
        AssertJUnit.assertFalse("should not replace its draw with a land with 8 lands in play", searches(setup(8, 0)));
    }

    private Player setup(int landsInPlay, int landsInHand) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Silver Deputy", ai);
        for (int i = 0; i < landsInPlay; i++) addCard(i % 2 == 0 ? "Plains" : "Mountain", ai);
        for (int i = 0; i < landsInHand; i++) addCardToZone("Plains", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        for (int i = 0; i < 4; i++) addCardToZone("Plains", ai, ZoneType.Library);
        for (int i = 0; i < 4; i++) addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        return ai;
    }

    private boolean searches(Player ai) {
        Card deputy = null;
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Silver Deputy")) {
                deputy = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(deputy);
        SpellAbility sa = null;
        for (Trigger t : deputy.getTriggers()) {
            SpellAbility ability = t.ensureAbility();
            if (ability != null && ability.getApi() == ApiType.ChangeZone) {
                sa = ability;
                break;
            }
        }
        AssertJUnit.assertNotNull("no search trigger on Silver Deputy", sa);
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, false);
    }
}
