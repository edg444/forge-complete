package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Leading Performance (UNF #18), checked against Scryfall and its 3 rulings 2026-10-21: two different letters, names
 * skip spaces and punctuation, and a creature gets one counter however many letters match.
 */
public class LeadingPerformanceTest extends AITest {

    @Test
    public void testTwoDifferentLettersAndOneCounterEach() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card elves1 = addCard("Llanowar Elves", ai);
        final Card elves2 = addCard("Llanowar Elves", ai);
        final Card bears1 = addCard("Grizzly Bears", ai);
        final Card bears2 = addCard("Grizzly Bears", ai);
        final Card giant = addCard("Hill Giant", ai);
        final Card goblin = addCard("_____ Goblin", ai);
        final Card theirs = addCard("Llanowar Elves", opp);

        final Card spell = addCardToZone("Leading Performance", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AbilityUtils.resolve(sa);

        AssertJUnit.assertEquals("G", spell.getChosenType());
        AssertJUnit.assertEquals("a different second letter", "L", spell.getChosenType2());
        for (final Card c : new Card[] {elves1, elves2, bears1, bears2, goblin}) {
            AssertJUnit.assertEquals(c.getName(), 1, c.getCounters(CounterEnumType.P1P1));
        }
        AssertJUnit.assertEquals(0, giant.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("only creatures you control", 0, theirs.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testTheAiWantsAtLeastTwoCreatures() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Hill Giant", ai);
        final Card spell = addCardToZone("Leading Performance", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay);
    }
}
