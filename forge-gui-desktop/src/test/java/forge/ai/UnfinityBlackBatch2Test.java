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
 * Nocturno of Myra's Marvels (UNF #82), Photo Op (UNF #83), Questionable Cuisine (UNF #84) and Rat in the Hat (UNF #86),
 * checked against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityBlackBatch2Test extends AITest {

    @Test
    public void testNocturnoExactWordInRulesText() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card nocturno = addCard("Nocturno of Myra's Marvels", p);
        nocturno.setChosenType("damage");
        final Card bolt = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        final Card divination = addCardToZone("Divination", p, ZoneType.Hand);
        AssertJUnit.assertTrue(bolt.isValid("Card.rulesTextHasChosenWord", p, nocturno, null));
        AssertJUnit.assertFalse(divination.isValid("Card.rulesTextHasChosenWord", p, nocturno, null));
        nocturno.setChosenType("damag");
        AssertJUnit.assertFalse("whole words only", bolt.isValid("Card.rulesTextHasChosenWord", p, nocturno, null));
    }

    @Test
    public void testQuestionableCuisine() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        final Card spell = addCardToZone("Questionable Cuisine", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Hand));
        final long food = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Food")).count();
        AssertJUnit.assertEquals("one Food per trash can", spell.getChosenNumber().longValue(), food);
        AssertJUnit.assertTrue(food <= 3);
    }

    @Test
    public void testRatInTheHatOnlyKnownHatsForTheAi() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rat = addCard("Rat in the Hat", ai);
        final Card hatted = addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        hatted.setHasHatClaim(true);
        final Card bare = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        SpellAbility sa = null;
        for (final SpellAbility s : rat.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(hatted);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue("may cast it from the graveyard",
                !hatted.mayPlay(ai).isEmpty() || !game.getCardState(hatted).mayPlay(ai).isEmpty());
        AssertJUnit.assertTrue("a creature without a hat isn't castable", game.getCardState(bare).mayPlay(ai).isEmpty());
    }

    @Test
    public void testPhotoOpExilesItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Photo Op", ai, ZoneType.Hand);
        final Card onStack = game.getAction().moveToStack(spell, spell.getFirstSpellAbility());
        final SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(onStack).isInZone(ZoneType.Exile));
    }
}
