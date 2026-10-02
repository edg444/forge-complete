package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Big Idea (Unstable): "{2}{B/R}{B/R}, {T}: Roll a six-sided die. Create a number of 1/1 red Brainiac creature
 * tokens equal to the result. / Tap three untapped Brainiacs you control: The next time you would roll a six-sided
 * die, instead roll two six-sided dice and use the total of those results." - Oracle text and the three rulings
 * (2018-01-19) checked against Scryfall 2026-10-01.
 */
public class TheBigIdeaTest extends AITest {

    private static SpellAbility ability(Card card, ApiType api, Player activator) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == api) {
                SpellAbility copy = sa.copy(card, activator, false);
                copy.setActivatingPlayer(activator);
                return copy;
            }
        }
        throw new AssertionError("no " + api + " ability on " + card);
    }

    private static int brainiacTokens(Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getType().hasCreatureType("Brainiac")).size();
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card idea = addCard("The Big Idea", me);
        AssertJUnit.assertEquals("{2}{B/R}{B/R}, {T}: Roll a six-sided die. Create a number of 1/1 red Brainiac creature tokens equal to the result. | "
                + "Tap three untapped Brainiacs you control: The next time you would roll a six-sided die, instead roll two six-sided dice and use the total of those results.",
                String.join(" | ", idea.getAbilityText().trim().split("\\s*[\\r\\n]+")));
    }

    @Test
    public void testOneDieWithoutTheShield() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card idea = addCard("The Big Idea", me);
        game.getAction().checkStateEffects(true);
        int rollsBefore = me.getNumRollsThisTurn();
        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        int tokens = brainiacTokens(me);
        AssertJUnit.assertTrue("tokens " + tokens, tokens >= 1 && tokens <= 6);
        AssertJUnit.assertEquals(rollsBefore + 1, me.getNumRollsThisTurn());
    }

    @Test
    public void testShieldRollsTwoDiceAndUsesTheTotalOnce() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card idea = addCard("The Big Idea", me);
        game.getAction().checkStateEffects(true);
        AbilityUtils.resolve(ability(idea, ApiType.Effect, me));
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Command).size());

        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        List<Integer> dice = me.getDiceRollsThisTurn();
        AssertJUnit.assertEquals(2, dice.size());
        AssertJUnit.assertEquals(2, me.getNumRollsThisTurn());
        AssertJUnit.assertEquals(dice.get(0) + dice.get(1), brainiacTokens(me));
        // used up
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Command).size());
        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        AssertJUnit.assertEquals(3, me.getNumRollsThisTurn());
    }

    @Test
    public void testTwoShieldsAddUpToOneResult() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card idea = addCard("The Big Idea", me);
        game.getAction().checkStateEffects(true);
        AbilityUtils.resolve(ability(idea, ApiType.Effect, me));
        AbilityUtils.resolve(ability(idea, ApiType.Effect, me));
        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        List<Integer> dice = me.getDiceRollsThisTurn();
        AssertJUnit.assertEquals(3, dice.size());
        AssertJUnit.assertEquals(dice.get(0) + dice.get(1) + dice.get(2), brainiacTokens(me));
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Command).size());
    }

    @Test
    public void testOnlyYourSixSidedDice() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card idea = addCard("The Big Idea", me);
        Card painiac = addCard("Painiac", opp);
        game.getAction().checkStateEffects(true);
        AbilityUtils.resolve(ability(idea, ApiType.Effect, me));

        // Painiac: "roll a six-sided die. This creature gets +X/+0 until end of turn"
        SpellAbility oppRoll = painiac.getTriggers().getFirst().ensureAbility().copy(painiac, opp, false);
        oppRoll.setActivatingPlayer(opp);
        AbilityUtils.resolve(oppRoll);
        AssertJUnit.assertEquals(1, opp.getNumRollsThisTurn());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Command).size());

        // a d20 isn't a six-sided die
        RollDiceEffect.rollDiceForPlayer(null, me, 1, 20);
        AssertJUnit.assertEquals(1, me.getNumRollsThisTurn());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Command).size());
    }

    @Test
    public void testEachDieTriggersOnItsOwn() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card idea = addCard("The Big Idea", me);
        // "Whenever you roll a die, put a +1/+1 counter on The Space Family Goblinson."
        addCard("The Space Family Goblinson", me);
        // "Whenever you roll your third die each turn, put a +1/+1 counter on this creature."
        addCard("Resolute Veggiesaur", me);
        game.getAction().checkStateEffects(true);

        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertEquals(1, game.getStack().size());
        game.getStack().clear();

        // dice two and three of the turn are the two rolled for one
        AbilityUtils.resolve(ability(idea, ApiType.Effect, me));
        AbilityUtils.resolve(ability(idea, ApiType.RollDice, me));
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertEquals(3, game.getStack().size());
    }

    @Test
    public void testAiShieldsBeforeItsOwnTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card idea = addCard("The Big Idea", ai);
        for (int i = 0; i < 3; i++) {
            addToken("r_1_1_brainiac", ai);
        }
        game.getAction().checkStateEffects(true);
        SpellAbility shield = null;
        for (SpellAbility sa : idea.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Effect) {
                shield = sa;
            }
        }
        shield.setActivatingPlayer(ai);
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, aic.canPlaySa(shield));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        boolean played = false;
        for (int i = 0; i < 20 && !played; i++) {
            played = aic.canPlaySa(shield) == AiPlayDecision.WillPlay;
        }
        AssertJUnit.assertTrue(played);
    }
}
