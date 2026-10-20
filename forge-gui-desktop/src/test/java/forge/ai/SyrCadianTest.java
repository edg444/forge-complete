package forge.ai;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.GameEntityCounterTable;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.SunTimes;

/**
 * Syr Cadian, Knight Owl (UND #15), checked against Scryfall and its 5 rulings 2026-10-20. User ruling: sunrise and
 * sunset at the reference city of the computer's time zone. Expected New York times from api.sunrise-sunset.org
 * (2026-10-20): 06-21 05:23/20:32 EDT, 12-21 07:14/16:33 EST.
 */
public class SyrCadianTest extends AITest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @AfterMethod
    public void restoreClock() {
        AbilityUtils.realWorldNow = ZonedDateTime::now;
    }

    private static void assertNear(final String what, final int hour, final int minute, final ZonedDateTime actual) {
        final ZonedDateTime expected = actual.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        final long off = Math.abs(Duration.between(expected, actual).toMinutes());
        AssertJUnit.assertTrue(what + " was " + actual.toLocalTime(), off <= 3);
    }

    private static ZonedDateTime at(final ZoneId zone, final int month, final int day, final int hour, final int minute) {
        return ZonedDateTime.of(LocalDateTime.of(2026, month, day, hour, minute), zone);
    }

    @Test
    public void newYorkSunriseAndSunset() {
        initAndCreateGame(); // loads the time zone list
        SunTimes.Day june = SunTimes.sunriseSunset(LocalDate.of(2026, 6, 21), NEW_YORK, 40.7142, -74.0064);
        assertNear("June sunrise", 5, 23, june.sunrise());
        assertNear("June sunset", 20, 32, june.sunset());
        SunTimes.Day december = SunTimes.sunriseSunset(LocalDate.of(2026, 12, 21), NEW_YORK, 40.7142, -74.0064);
        assertNear("December sunrise", 7, 14, december.sunrise());
        assertNear("December sunset", 16, 33, december.sunset());

        AssertJUnit.assertTrue(SunTimes.isSunUp(at(NEW_YORK, 6, 21, 12, 0)));
        AssertJUnit.assertFalse(SunTimes.isSunDown(at(NEW_YORK, 6, 21, 12, 0)));
        AssertJUnit.assertTrue(SunTimes.isSunDown(at(NEW_YORK, 6, 21, 23, 0)));
        AssertJUnit.assertFalse(SunTimes.isSunUp(at(NEW_YORK, 6, 21, 23, 0)));
    }

    @Test
    public void bothOnTheExactMinute() {
        initAndCreateGame();
        ZonedDateTime sunset = SunTimes.sunriseSunset(LocalDate.of(2026, 6, 21), NEW_YORK, 40.7142, -74.0064).sunset();
        AssertJUnit.assertTrue(SunTimes.isSunUp(sunset.plusSeconds(59)));
        AssertJUnit.assertTrue(SunTimes.isSunDown(sunset.plusSeconds(59)));
        AssertJUnit.assertFalse(SunTimes.isSunDown(sunset.minusMinutes(1)));
        AssertJUnit.assertFalse(SunTimes.isSunUp(sunset.plusMinutes(1)));
    }

    @Test
    public void midnightSunAndZonesWithoutACity() {
        initAndCreateGame();
        ZoneId svalbard = ZoneId.of("Arctic/Longyearbyen");
        AssertJUnit.assertTrue(SunTimes.isSunUp(at(svalbard, 6, 21, 0, 30)));
        AssertJUnit.assertFalse(SunTimes.isSunDown(at(svalbard, 6, 21, 0, 30)));
        AssertJUnit.assertTrue("polar night", SunTimes.isSunDown(at(svalbard, 12, 21, 12, 0)));
        ZoneId utc = ZoneId.of("Etc/UTC");
        AssertJUnit.assertTrue(SunTimes.isSunUp(at(utc, 6, 21, 12, 0)));
        AssertJUnit.assertTrue(SunTimes.isSunDown(at(utc, 6, 21, 18, 0)));
        AssertJUnit.assertTrue(SunTimes.isSunUp(at(utc, 6, 21, 18, 0)));
    }

    @Test
    public void abilitiesFollowTheSun() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card cadian = addCard("Syr Cadian, Knight Owl", ai);
        SpellAbility vigilance = cadian.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.Pump && s.getParam("KW").equals("Vigilance")).findFirst().orElseThrow();
        SpellAbility flying = cadian.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.Pump && s.getParam("KW").equals("Flying")).findFirst().orElseThrow();
        vigilance.setActivatingPlayer(ai);
        flying.setActivatingPlayer(ai);

        AbilityUtils.realWorldNow = () -> at(NEW_YORK, 6, 21, 12, 0);
        AssertJUnit.assertTrue(vigilance.getRestrictions().canPlay(cadian, vigilance));
        AssertJUnit.assertFalse(flying.getRestrictions().canPlay(cadian, flying));
        AbilityUtils.realWorldNow = () -> at(NEW_YORK, 6, 21, 23, 0);
        AssertJUnit.assertFalse(vigilance.getRestrictions().canPlay(cadian, vigilance));
        AssertJUnit.assertTrue(flying.getRestrictions().canPlay(cadian, flying));
    }

    @Test
    public void knightlifelink() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card cadian = addCard("Syr Cadian, Knight Owl", ai);
        Card knight = addCard("Knight Errant", ai);
        Card lifelinkKnight = addCard("Knight of Meadowgrain", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card theirKnight = addCard("Knight Errant", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("knightlifelink isn't lifelink", knight.hasKeyword(Keyword.LIFELINK));

        CardDamageTable damage = new CardDamageTable();
        damage.put(cadian, opp, 4);
        damage.put(knight, opp, 2);
        damage.put(lifelinkKnight, opp, 2);
        damage.put(bears, opp, 2);
        damage.put(theirKnight, ai, 2);
        game.getAction().dealDamage(false, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        AssertJUnit.assertEquals("Cadian 4 + Knight 2 + lifelink Knight 2 (once), not the Bears", 20 - 2 + 8, ai.getLife());
        AssertJUnit.assertEquals("an opponent's Knight gains them nothing", 20 - 10, opp.getLife());
    }
}
