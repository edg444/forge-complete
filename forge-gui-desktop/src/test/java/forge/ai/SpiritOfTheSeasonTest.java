package forge.ai;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Seasons;
import forge.util.Seasons.Season;

/**
 * Spirit of the Season (UND #70), checked against Scryfall and its 3 rulings 2026-10-20. User ruling: astronomical
 * seasons, swapped in the southern hemisphere. Equinoxes and solstices of 2026 from the USNO (aa.usno.navy.mil,
 * fetched 2026-10-20): Mar 20 14:46, Jun 21 08:24, Sep 23 00:05, Dec 21 20:50 UTC.
 */
public class SpiritOfTheSeasonTest extends AITest {

    @AfterMethod
    public void restoreClock() {
        AbilityUtils.realWorldNow = ZonedDateTime::now;
    }

    private static void turnsAt(final String utc, final Season before, final Season after) {
        final Instant t = Instant.parse(utc);
        AssertJUnit.assertEquals(utc, before, Seasons.season(t.minus(Duration.ofMinutes(30)), false));
        AssertJUnit.assertEquals(utc, after, Seasons.season(t.plus(Duration.ofMinutes(30)), false));
    }

    @Test
    public void seasonsTurnAtTheUsnoTimes() {
        turnsAt("2026-03-20T14:46:00Z", Season.WINTER, Season.SPRING);
        turnsAt("2026-06-21T08:24:00Z", Season.SPRING, Season.SUMMER);
        turnsAt("2026-09-23T00:05:00Z", Season.SUMMER, Season.AUTUMN);
        turnsAt("2026-12-21T20:50:00Z", Season.AUTUMN, Season.WINTER);
    }

    @Test
    public void southernHemisphereIsSwapped() {
        initAndCreateGame(); // loads the time zone list
        final ZonedDateTime july = ZonedDateTime.of(2026, 7, 15, 12, 0, 0, 0, ZoneId.of("Australia/Sydney"));
        AssertJUnit.assertEquals(Season.WINTER, Seasons.seasonAt(july));
        AssertJUnit.assertEquals(Season.SUMMER, Seasons.seasonAt(july.withZoneSameInstant(ZoneId.of("America/New_York"))));
    }

    private Card enter(final Game game, final Player p) {
        game.getAction().checkStateEffects(true);
        Card c = addCardToZone("Spirit of the Season", p, ZoneType.Hand);
        c = game.getAction().moveToPlay(c, p, null, null);
        playUntilStackClear(game);
        return game.getCardState(c);
    }

    private Game at(final int month) {
        AbilityUtils.realWorldNow = () -> ZonedDateTime.of(2026, month, 15, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void eachSeason() {
        Game game = at(7);
        Player ai = game.getPlayers().get(1);
        Card s = enter(game, ai);
        AssertJUnit.assertTrue("summer: haste", s.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertEquals(0, s.getCounters(CounterEnumType.P1P1));

        game = at(10);
        ai = game.getPlayers().get(1);
        s = enter(game, ai);
        AssertJUnit.assertEquals("autumn: a counter", 1, s.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertFalse(s.hasKeyword(Keyword.HASTE));

        game = at(1);
        ai = game.getPlayers().get(1);
        enter(game, ai);
        AssertJUnit.assertEquals("winter: 5 life", 25, ai.getLife());

        game = at(4);
        ai = game.getPlayers().get(1);
        addCardToZone("Forest", ai, ZoneType.Library);
        enter(game, ai);
        AssertJUnit.assertEquals("spring: a basic land", 1, (int) ai.getCardsIn(ZoneType.Hand).stream().filter(c -> c.getName().equals("Forest")).count());
    }
}
