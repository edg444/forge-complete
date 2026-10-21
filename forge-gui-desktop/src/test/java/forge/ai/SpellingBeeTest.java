package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SpellChosenWordAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.SpellChosenWordEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spelling Bee (UNF #155), checked against Scryfall and its rulings 2026-10-21: any word from the card's name, type line or
 * text box, real or not; drawing the card if spelled correctly, otherwise scrying 1.
 */
public class SpellingBeeTest extends AITest {

    @Test
    public void testWordsOnACard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final List<String> words = SpellChosenWordEffect.wordsOn(addCardToZone("Llanowar Elves", p, ZoneType.Library));
        AssertJUnit.assertTrue(words.toString(), words.contains("Llanowar"));
        AssertJUnit.assertTrue(words.toString(), words.contains("Creature"));
        AssertJUnit.assertTrue(words.toString(), words.contains("Add"));
        AssertJUnit.assertEquals("Llanowar", new SpellChosenWordAi().chooseString(p, null, List.of("Add", "Llanowar", "Elf")));
        AssertJUnit.assertTrue(SpellChosenWordEffect.aiSpellingChance("Elf") > SpellChosenWordEffect.aiSpellingChance("Astroquarium"));
    }

    @Test
    public void testDrawOrScry() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card bee = addCard("Spelling Bee", ai);
        final SpellAbility trig = AbilityFactory.getAbility(bee.getSVar("TrigSpell"), bee);
        trig.setActivatingPlayer(ai);
        trig.putParam("Chooser", "Opponent");
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(trig);
        final boolean correct = bee.getSVar("SpelledCorrectly").equals("Number$1");
        AssertJUnit.assertEquals(correct ? hand + 1 : hand, ai.getCardsIn(ZoneType.Hand).size());
    }
}
