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
 * The Grand Tour (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: any
 * permanent may be targeted; a token stops at exile, anything else comes back to the battlefield as a new object.
 */
public class TheGrandTourTest extends AITest {

    @Test
    public void testTourAndTokenStop() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        Card token = addToken("w_1_1_human_soldier", ai);

        Card spell = addCardToZone("The Grand Tour", ai, ZoneType.Hand);
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sa.canTarget(token));
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Library).isEmpty());

        SpellAbility again = addCardToZone("The Grand Tour", ai, ZoneType.Hand).getFirstSpellAbility();
        again.setActivatingPlayer(ai);
        again.getTargets().add(token);
        AbilityUtils.resolve(again);
        AssertJUnit.assertFalse(ai.getCardsIn(ZoneType.Battlefield).anyMatch(Card::isToken));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Library).isEmpty());
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
    }
}
