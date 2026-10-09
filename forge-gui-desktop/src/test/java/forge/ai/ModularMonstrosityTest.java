package forge.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.keyword.PrintedCreatureKeywords;
import forge.game.player.KeywordDayLog;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/**
 * Modular Monstrosity (Unstable) - Oracle text and the four Scryfall rulings checked 2026-10-09.
 */
public class ModularMonstrosityTest extends AITest {

    // keep the tests out of the real day log in the user folder
    @BeforeMethod
    public void freshDay() {
        KeywordDayLog.setStore(new KeywordDayLog.Store() {
            private final Map<String, Set<String>> chosen = new HashMap<>();
            @Override
            public Set<String> chosen(final String person, final String day) {
                return new HashSet<>(chosen.getOrDefault(person + day, Set.of()));
            }
            @Override
            public void add(final String person, final String day, final String keyword) {
                chosen.computeIfAbsent(person + day, k -> new HashSet<>()).add(keyword);
            }
        });
    }

    private void opponentCasts(final Card monstrosity, final Player me) {
        for (final Trigger t : monstrosity.getTriggers()) {
            final SpellAbility sa = t.ensureAbility();
            sa.setActivatingPlayer(me);
            AbilityUtils.resolve(sa);
        }
    }

    @Test
    public void aDifferentKeywordEachTime() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card monstrosity = addCard("Modular Monstrosity", me);

        opponentCasts(monstrosity, me);
        AssertJUnit.assertTrue(monstrosity.hasKeyword(Keyword.FLYING));
        opponentCasts(monstrosity, me);
        AssertJUnit.assertTrue(monstrosity.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertTrue(monstrosity.hasKeyword(Keyword.FLYING));

        // "every Modular Monstrosity you play with that day": a second one can't have flying or double strike
        final Card another = addCard("Modular Monstrosity", me);
        opponentCasts(another, me);
        AssertJUnit.assertFalse(another.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(another.hasKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Test
    public void tooSlowLosesEveryKeyword() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card monstrosity = addCard("Modular Monstrosity", me);
        opponentCasts(monstrosity, me);
        AssertJUnit.assertTrue(monstrosity.hasKeyword(Keyword.FLYING));

        // someone who doesn't choose in time
        me.addController(game.getNextTimestamp(), me, new PlayerControllerAi(game, me, me.getLobbyPlayer()) {
            @Override
            public String chooseKeywordInTime(final List<String> keywords, final int seconds, final SpellAbility sa) {
                return null;
            }
        }, false);
        opponentCasts(monstrosity, me);
        AssertJUnit.assertFalse(monstrosity.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(monstrosity.getKeywords().isEmpty());
    }

    @Test
    public void onlyPrintedForms() {
        initAndCreateGame();
        final List<String> all = PrintedCreatureKeywords.all();
        final Set<String> titles = new HashSet<>();
        for (final String k : all) {
            titles.add(PrintedCreatureKeywords.title(k).toLowerCase());
            AssertJUnit.assertNotSame(Keyword.MAYFLASHCOST, PrintedCreatureKeywords.keyword(k));
        }
        AssertJUnit.assertTrue(titles.contains("flying"));
        // Ulamog's Crusher and friends
        AssertJUnit.assertTrue(titles.contains("annihilator 2"));
        // "Sorry, 'annihilator 500' fans."
        AssertJUnit.assertFalse(titles.contains("annihilator 500"));
    }
}
