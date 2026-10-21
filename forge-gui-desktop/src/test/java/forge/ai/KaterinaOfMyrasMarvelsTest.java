package forge.ai;

import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.NameLetters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Katerina of Myra's Marvels (UNF #16), checked against Scryfall and its ruling 2026-10-21: a name begins with its
 * first letter once spaces and punctuation, blank lines included, are skipped.
 */
public class KaterinaOfMyrasMarvelsTest extends AITest {

    @Test
    public void testInitialSkipsSpacesAndPunctuation() {
        AssertJUnit.assertEquals('L', NameLetters.initial("Lightning Bolt"));
        AssertJUnit.assertEquals('G', NameLetters.initial("_____ Goblin"));
        AssertJUnit.assertEquals('A', NameLetters.initial("\"Ach! Hans, Run!\""));
        AssertJUnit.assertEquals("a blank is no name (Stet)", -1, NameLetters.initial("____________"));
        AssertJUnit.assertEquals("a digit isn't a letter", -1, NameLetters.initial("+2 Mace"));
        AssertJUnit.assertEquals(-1, NameLetters.initial(""));
    }

    @Test
    public void testSpellsBeginningWithTheChosenLetterMakeCats() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card katerina = addCard("Katerina of Myra's Marvels", p);
        katerina.setChosenType("L");
        final Card bolt = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        final Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        final String valid = katerina.getTriggers().get(0).getParam("ValidCard");
        AssertJUnit.assertTrue(bolt.isValid(valid, p, katerina, null));
        AssertJUnit.assertFalse(growth.isValid(valid, p, katerina, null));

        final SpellAbility cat = katerina.getTriggers().get(0).ensureAbility();
        cat.setActivatingPlayer(p);
        AbilityUtils.resolve(cat);
        final Card token = p.getCreaturesInPlay().stream().filter(Card::isToken).findFirst().orElse(null);
        AssertJUnit.assertNotNull(token);
        AssertJUnit.assertEquals(2, token.getNetPower());
        AssertJUnit.assertTrue(token.hasKeyword(forge.game.keyword.Keyword.FLYING));
        AssertJUnit.assertTrue(token.getType().hasCreatureType("Cat"));
    }

    @Test
    public void testTheAiPicksTheCommonestInitialAmongItsSpells() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Llanowar Elves", ai, ZoneType.Library);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        addCardToZone("Gigantosaurus", ai, ZoneType.Library);
        addCardToZone("Lava Spike", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        addCardToZone("Fog", ai, ZoneType.Library);
        final Card katerina = addCard("Katerina of Myra's Marvels", ai);
        final SpellAbility choose = AbilityFactory.getAbility(katerina.getSVar("DBChooseLetter"), katerina);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("L", ComputerUtil.chooseSomeType(ai, "letter", choose,
                Arrays.asList(choose.getParam("ValidTypes").split(","))));
    }
}
