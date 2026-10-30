package forge.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.CommanderBracketCalculator;
import forge.deck.CommanderBracketEnforcement;
import forge.deck.CommanderBracketEnforcement.Mode;
import forge.deck.CommanderBracketEnforcement.Rule;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/** The Commander bracket rule enforced at match start, apart from deck conformance. */
public class CommanderBracketEnforcementTest extends AITest {

    private static Deck deck(final String name, final String commander, final String... cards) {
        final Deck deck = new Deck(name);
        deck.getOrCreate(DeckSection.Commander).add(commander, 1);
        for (final String card : cards) {
            deck.getMain().add(card, 1);
        }
        return deck;
    }

    /** Four Game Changers: bracket 4. */
    private static Deck fastMana(final String name) {
        return deck(name, "Krenko, Mob Boss", "Mana Vault", "Grim Monolith", "Ancient Tomb", "Chrome Mox");
    }

    private static Deck plain(final String name) {
        return deck(name, "Krenko, Mob Boss", "Mountain", "Goblin Piker");
    }

    @Test
    public void rulesParse() {
        initAndCreateGame();
        AssertJUnit.assertEquals(Mode.OFF, Rule.parse("Off").mode());
        AssertJUnit.assertEquals(new Rule(Mode.MAX, 3), Rule.parse("Max 3"));
        AssertJUnit.assertEquals(Mode.ALL_MATCH, Rule.parse("All Match").mode());
        AssertJUnit.assertEquals("Max 5 is no limit", Mode.OFF, Rule.parse("Max 5").mode());
        AssertJUnit.assertEquals(Mode.OFF, Rule.parse("garbage").mode());
    }

    @Test
    public void maxFlagsOnlyTheDeckOverIt() {
        initAndCreateGame();
        AssertJUnit.assertEquals(4, CommanderBracketCalculator.getBracket(fastMana("x")));
        AssertJUnit.assertEquals(1, CommanderBracketCalculator.getBracket(plain("x")));
        final Map<String, Deck> decks = new LinkedHashMap<>();
        decks.put("Alice", fastMana("A"));
        decks.put("Bob", plain("B"));
        final List<String> problems = CommanderBracketEnforcement.problems(Rule.parse("Max 3"), decks);
        AssertJUnit.assertEquals(1, problems.size());
        AssertJUnit.assertTrue(problems.get(0), problems.get(0).startsWith("Alice: bracket 4"));
        AssertJUnit.assertTrue("names what put it there", problems.get(0).contains("Mana Vault"));
        AssertJUnit.assertTrue(CommanderBracketEnforcement.problems(Rule.parse("Max 4"), decks).isEmpty());
        AssertJUnit.assertTrue(CommanderBracketEnforcement.problems(Rule.parse("Off"), decks).isEmpty());
    }

    @Test
    public void allMatchFlagsEveryDeckWhenTheyDiffer() {
        initAndCreateGame();
        final Map<String, Deck> decks = new LinkedHashMap<>();
        decks.put("Alice", fastMana("A"));
        decks.put("Bob", plain("B"));
        AssertJUnit.assertEquals(2, CommanderBracketEnforcement.problems(Rule.parse("All Match"), decks).size());
        decks.put("Alice", plain("A"));
        AssertJUnit.assertTrue(CommanderBracketEnforcement.problems(Rule.parse("All Match"), decks).isEmpty());
        AssertJUnit.assertEquals(Integer.valueOf(1), CommanderBracketEnforcement.sharedTarget(decks.values()));
    }

    @Test
    public void capLowersTheGeneratorsMaximum() {
        initAndCreateGame();
        final ForgePreferences prefs = FModel.getPreferences();
        final String before = prefs.getPref(FPref.ENFORCE_COMMANDER_BRACKET);
        try {
            prefs.setPref(FPref.ENFORCE_COMMANDER_BRACKET, "Max 2");
            AssertJUnit.assertEquals(2, CommanderBracketEnforcement.generationCap(5));
            AssertJUnit.assertEquals(1, CommanderBracketEnforcement.generationCap(1));
            prefs.setPref(FPref.ENFORCE_COMMANDER_BRACKET, "All Match");
            AssertJUnit.assertEquals(5, CommanderBracketEnforcement.generationCap(5));
        } finally {
            prefs.setPref(FPref.ENFORCE_COMMANDER_BRACKET, before);
        }
    }

    @Test
    public void generatedAiDeckIsMadeAgainUnderTheCap() {
        initAndCreateGame();
        final Deck generated = fastMana("Generated Commander deck (Krenko, Mob Boss)");
        AssertJUnit.assertTrue(CommanderBracketEnforcement.isGenerated(generated));
        final Deck refit = CommanderBracketEnforcement.refitGenerated(generated, DeckFormat.Commander, Rule.parse("Max 2"), null);
        AssertJUnit.assertNotSame(generated, refit);
        AssertJUnit.assertTrue(CommanderBracketCalculator.getBracket(refit) <= 2);
        AssertJUnit.assertEquals("Krenko, Mob Boss", refit.getCommanders().get(0).getName());
        // a deck the player picked is never swapped out
        final Deck picked = fastMana("Krenko Goblins");
        AssertJUnit.assertSame(picked, CommanderBracketEnforcement.refitGenerated(picked, DeckFormat.Commander, Rule.parse("Max 2"), null));
    }
}
