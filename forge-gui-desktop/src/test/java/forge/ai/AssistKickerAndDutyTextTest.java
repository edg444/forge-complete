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
 * Card text of two cards whose scripts lean on generic machinery: All-Star Kicker (Assist kicker, scripted as Kicker
 * plus an AssistKicker SVar) and Immortal Obligation (duty counter statics that name creatures its caster controls).
 */
public class AssistKickerAndDutyTextTest extends AITest {

    @Test
    public void testAssistKickerShowsNoPlainKicker() {
        Game game = initAndCreateGame();
        Card kicker = addCardToZone("All-Star Kicker", game.getPlayers().get(1), ZoneType.Hand);
        final String text = kicker.getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Assist kicker {3}"));
        AssertJUnit.assertFalse(text, text.replace("Assist kicker", "").contains("Kicker {3}"));
        AssertJUnit.assertFalse(text, text.replace("Assist kicker", "").replace("All-Star Kicker", "").contains("Kicker"));
    }

    @Test
    public void testPlainKickerStillShown() {
        Game game = initAndCreateGame();
        Card burst = addCardToZone("Burst Lightning", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertTrue(burst.getAbilityText(), burst.getAbilityText().contains("Kicker {4}"));
    }

    @Test
    public void testDutyTextOnlyOnTheDutyCreature() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);
        Card obligation = addCardToZone("Immortal Obligation", ai, ZoneType.Hand);

        SpellAbility sa = obligation.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        Card returned = game.getCardState(bears);
        AssertJUnit.assertTrue(returned.isInZone(ZoneType.Battlefield));

        Card mine = addCard("Hill Giant", ai);
        AssertJUnit.assertFalse(mine.getAbilityText(), mine.getAbilityText().contains("duty counter"));
        AssertJUnit.assertFalse(mine.getAbilityText(), mine.getAbilityText().contains("can't block"));
    }
}
