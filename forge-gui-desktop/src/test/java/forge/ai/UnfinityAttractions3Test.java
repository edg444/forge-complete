package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.SuperlativeChoiceEffect;
import forge.game.ability.effects.TriviaContestEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Memory Test (UNF #221), Scavenger Hunt (UNF #226), The Superlatorium (UNF #230) and Trivia Contest (UNF #233), checked
 * against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityAttractions3Test extends AITest {

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

    private SpellAbility ability(final Card attraction, final String svar, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(attraction.getSVar(svar), attraction);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testMemoryTest() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        // top to bottom: Island, Bears, Island, five times - exiling from the bottom until five nonland cards leaves
        // only the top Island
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
            addCardToZone("Grizzly Bears", opp, ZoneType.Library);
            addCardToZone("Island", opp, ZoneType.Library);
        }
        final Card test = attraction("Memory Test", ai);
        final SpellAbility exile = ability(test, "TrigExile", ai);
        exile.getTargets().add(opp);
        AbilityUtils.resolve(exile);
        AssertJUnit.assertEquals("five nonland cards face down", 5,
                CardLists.filter(opp.getCardsIn(ZoneType.Exile), Card::isFaceDown).size());
        AssertJUnit.assertEquals("the lands exiled along the way stay face up", 9,
                CardLists.filter(opp.getCardsIn(ZoneType.Exile), c -> !c.isFaceDown() && c.isLand()).size());
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Library).size());
        AbilityUtils.resolve(ability(test, "TrigName", ai));
        resolveAll(game);
        final boolean named = test.getSVar("NamedAll").equals("Number$1");
        AssertJUnit.assertEquals(named ? 0 : 5, CardLists.filter(opp.getCardsIn(ZoneType.Exile), Card::isFaceDown).size());
        AssertJUnit.assertEquals("every card stays in exile", 14, opp.getCardsIn(ZoneType.Exile).size());
    }

    @Test
    public void testScavengerHunt() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card hunt = attraction("Scavenger Hunt", ai);
        AbilityUtils.resolve(ability(hunt, "TrigHunt", ai));
        resolveAll(game);
        AssertJUnit.assertEquals("an Island shows none of the items", "Number$0", hunt.getSVar("Found"));
        AssertJUnit.assertTrue(hunt.isInPlay());
    }

    @Test
    public void testSuperlatorium() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card angel = addCard("Serra Angel", ai);
        AssertJUnit.assertTrue("Serra Angel flies", SuperlativeChoiceEffect.fit("the fastest").applyAsDouble(angel)
                > SuperlativeChoiceEffect.fit("the fastest").applyAsDouble(bears));
        AssertJUnit.assertTrue(SuperlativeChoiceEffect.fit("the weakest").applyAsDouble(bears)
                > SuperlativeChoiceEffect.fit("the weakest").applyAsDouble(angel));
        final Card sup = attraction("The Superlatorium", ai);
        AbilityUtils.resolve(ability(sup, "TrigAsk", ai));
        resolveAll(game);
        final boolean same = sup.getSVar("SameChoice").equals("Number$1");
        AssertJUnit.assertEquals(same ? 2 : 0, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                c -> c.getType().hasCreatureType("Balloon")).size());
    }

    @Test
    public void testTriviaContest() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCardToZone("Llanowar Elves", opp, ZoneType.Library);
        AssertJUnit.assertTrue("Liliana of the Veil", TriviaContestEffect.hasAnswer("planeswalker card", 'L'));
        final Card trivia = attraction("Trivia Contest", ai);
        AbilityUtils.resolve(ability(trivia, "TrigTrivia", ai));
        resolveAll(game);
        AssertJUnit.assertEquals("the opponent milled", 1, opp.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(trivia.getSVar("TriviaRight").equals("Number$1"), !trivia.isInPlay());
    }
}
