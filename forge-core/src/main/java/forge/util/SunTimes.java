package forge.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Real-world sunrise and sunset where the player is, for Unsanctioned's Syr Cadian, Knight Owl ("Activate only from
 * sunrise to sunset"). The place is the reference city of the computer's time zone (user ruling), from
 * res/lists/TimeZoneCoordinates.txt (IANA tz database); the times are NOAA's general solar position equations
 * (gml.noaa.gov/grad/solcalc/solareqns.PDF), good to a minute or two. A zone with no city (Etc/UTC and the like)
 * falls back to 6 AM - 6 PM, Nightmare Moon's daytime.
 */
public final class SunTimes {
    private SunTimes() { }

    private static Supplier<List<String>> source = null;
    private static Map<String, double[]> coordinates = null;

    /** Set by the GUI layer at startup; the file isn't read until something asks for it. */
    public static synchronized void setSource(final Supplier<List<String>> lineSource) {
        source = lineSource;
        coordinates = null;
    }

    static synchronized double[] coordinatesOf(final String zone) {
        if (coordinates == null) {
            coordinates = new HashMap<>();
            if (source != null) {
                for (final String raw : source.get()) {
                    final String[] f = raw.trim().split("\t");
                    if (f.length == 3 && !f[0].startsWith("#")) {
                        coordinates.put(f[0], new double[] {Double.parseDouble(f[1]), Double.parseDouble(f[2])});
                    }
                }
            }
        }
        return coordinates.get(zone);
    }

    /** Sunrise and sunset on one local date, to the minute; both null when the sun stays up or down all day. */
    public record Day(ZonedDateTime sunrise, ZonedDateTime sunset, boolean sunUpAllDay) { }

    public static Day sunriseSunset(final LocalDate date, final ZoneId zone, final double lat, final double lon) {
        final double gamma = 2 * Math.PI / (date.isLeapYear() ? 366 : 365) * (date.getDayOfYear() - 1);
        final double eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));
        final double decl = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma);
        final double latRad = Math.toRadians(lat);
        final double cosHa = Math.cos(Math.toRadians(90.833)) / (Math.cos(latRad) * Math.cos(decl))
                - Math.tan(latRad) * Math.tan(decl);
        if (cosHa > 1) {
            return new Day(null, null, false); // polar night
        }
        if (cosHa < -1) {
            return new Day(null, null, true); // midnight sun
        }
        final double ha = Math.toDegrees(Math.acos(cosHa));
        // the minutes are UTC on that date, so a far-east or far-west place's sunrise can land on the UTC day before
        final Instant utcMidnight = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant rise = utcMidnight.plusSeconds(Math.round((720 - 4 * (lon + ha) - eqTime) * 60));
        final Instant set = utcMidnight.plusSeconds(Math.round((720 - 4 * (lon - ha) - eqTime) * 60));
        return new Day(rise.atZone(zone).truncatedTo(ChronoUnit.MINUTES),
                set.atZone(zone).truncatedTo(ChronoUnit.MINUTES), false);
    }

    /** From sunrise to sunset, both minutes included (rulings: on the exact minute, both abilities work). */
    public static boolean isSunUp(final ZonedDateTime now) {
        return isBetween(now, true);
    }

    /** From sunset to sunrise, both minutes included. */
    public static boolean isSunDown(final ZonedDateTime now) {
        return isBetween(now, false);
    }

    private static boolean isBetween(final ZonedDateTime now, final boolean day) {
        final ZonedDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);
        final double[] at = coordinatesOf(now.getZone().getId());
        final ZonedDateTime rise;
        final ZonedDateTime set;
        if (at == null) {
            rise = minute.withHour(6).withMinute(0);
            set = minute.withHour(18).withMinute(0);
        } else {
            final Day times = sunriseSunset(now.toLocalDate(), now.getZone(), at[0], at[1]);
            if (times.sunrise() == null) {
                return day == times.sunUpAllDay();
            }
            rise = times.sunrise();
            set = times.sunset();
        }
        if (day) {
            return !minute.isBefore(rise) && !minute.isAfter(set);
        }
        return !minute.isAfter(rise) || !minute.isBefore(set);
    }
}
