package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Guess Your Fate (UNF #215), Push Your Luck (UNF #224) and Squirrel Stack (UNF #228), checked against Scryfall and
 * their rulings 2026-10-21.
 */
public class UnfinityAttractions2Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private Card attraction(final String name, final Player p) {
        final Card c = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard(name), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private void visit(final Card attraction, final String svar, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(attraction.getSVar(svar), attraction);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        resolveAll(p.getGame());
    }

    private int creatures(final Player p, final String type) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType(type)).size();
    }

    @Test
    public void testGuessYourFate() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Llanowar Elves", ai, ZoneType.Library);
        final Card fate = attraction("Guess Your Fate", ai);
        visit(fate, "TrigLook", ai);
        final boolean claimed = !fate.isInPlay();
        AssertJUnit.assertEquals("the bottom card comes to hand with the prize", claimed ? 1 : 0, ai.getCardsIn(ZoneType.Hand).size());
        if (claimed) {
            AssertJUnit.assertEquals("Llanowar Elves", ai.getCardsIn(ZoneType.Hand).getFirst().getName());
        }
    }

    @Test
    public void testPushYourLuck() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (final String name : new String[] {"Serra Angel", "Grizzly Bears", "Llanowar Elves"}) {
            addCardToZone(name, ai, ZoneType.Library);
        }
        final Card luck = attraction("Push Your Luck", ai);
        visit(luck, "TrigReveal", ai);
        final int total = Integer.parseInt(luck.getSVar("RevealedMV").replace("Number$", ""));
        AssertJUnit.assertTrue("at least one revealed", total > 0);
        AssertJUnit.assertEquals(total <= 7 ? 1 : 0, creatures(ai, "Teddy Bear"));
        AssertJUnit.assertEquals("claimed only on exactly 7", total == 7, !luck.isInPlay());
        AssertJUnit.assertEquals("still in the library", 3, ai.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testSquirrelStack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stack = attraction("Squirrel Stack", ai);
        visit(stack, "TrigStack", ai);
        final int dice = stack.getChosenNumber();
        AssertJUnit.assertEquals(dice >= 5 ? dice - 4 : 0, creatures(ai, "Squirrel"));
        AssertJUnit.assertEquals(dice < 5, stack.isInPlay());
    }
}
