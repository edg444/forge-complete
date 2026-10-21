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
 * Decisions, Decisions (UNF #44), checked against Scryfall and its 2 rulings 2026-10-21: reveal three, then choose a
 * consonant (Y counts); the ones with it in the first word of their name go to hand, the rest to the bottom.
 */
public class DecisionsDecisionsTest extends AITest {

    @Test
    public void testTheChosenLetterInTheFirstWord() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card spell = addCardToZone("Decisions, Decisions", p, ZoneType.Hand);
        spell.setChosenType("Y");
        final Card grizzly = addCardToZone("Grizzly Bears", p, ZoneType.Library);
        final Card lavaSpike = addCardToZone("Lava Spike", p, ZoneType.Library);
        AssertJUnit.assertTrue(grizzly.isValid("Card.firstWordHasChosenLetter", p, spell, null));
        AssertJUnit.assertFalse("only the first word", lavaSpike.isValid("Card.firstWordHasChosenLetter", p, spell, null));
    }

    @Test
    public void testTheAiTakesTheBestLetter() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        // top three: Island, Forest, Grizzly Bears - no consonant is in all three first words, several in two
        final Card spell = addCardToZone("Decisions, Decisions", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("two of the three", hand + 2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("the fourth card is now on top", "Lightning Bolt", ai.getCardsIn(ZoneType.Library).get(0).getName());
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(0, spell.getRememberedCount());
    }
}
