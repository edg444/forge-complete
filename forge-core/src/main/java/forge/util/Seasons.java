package forge.util;

import java.time.Instant;

/**
 * Unsanctioned's Spirit of the Season: the real-world season where the player is. Astronomical seasons (user ruling):
 * they turn at the equinoxes and solstices, found from the Sun's apparent ecliptic longitude (the Astronomical Almanac's
 * low-precision formula, good to about 0.01 degrees - a quarter of an hour). Spring is 0-90 degrees, summer 90-180,
 * autumn 180-270, winter 270-360, swapped south of the equator; the place is the time zone's reference city
 * (SunTimes), northern when the zone has none.
 */
public final class Seasons {
    private Seasons() { }

    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    /** The Sun's apparent ecliptic longitude at that instant, in degrees from the March equinox. */
    public static double sunLongitude(final Instant at) {
        final double n = at.getEpochSecond() / 86400.0 + 2440587.5 - 2451545.0;
        final double l = 280.460 + 0.9856474 * n;
        final double g = Math.toRadians(357.528 + 0.9856003 * n);
        final double lambda = l + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g);
        return ((lambda % 360) + 360) % 360;
    }

    public static Season season(final Instant at, final boolean southern) {
        final int quarter = (int) (sunLongitude(at) / 90) % 4;
        return Season.values()[southern ? (quarter + 2) % 4 : quarter];
    }

    public static Season seasonAt(final java.time.ZonedDateTime now) {
        final double[] at = SunTimes.coordinatesOf(now.getZone().getId());
        return season(now.toInstant(), at != null && at[0] < 0);
    }
}
