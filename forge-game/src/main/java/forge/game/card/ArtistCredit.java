package forge.game.card;

import java.util.ArrayList;
import java.util.List;

/**
 * Artist credits as the "choose an artist" cards read them. A printing can credit several artists ("A & B"), and
 * older silver-bordered printings credit nicknames (Rebecca "Don't Mess with Me" Guay) that the Unstable FAQ says to
 * ignore (Ineffable Blessing) - so the chosen artist is a person, not a credit line.
 */
public final class ArtistCredit {
    private ArtistCredit() { }

    /** Each artist a credit line names, without nicknames: "A & B" gives A and B. */
    public static List<String> individuals(final String credit) {
        final List<String> out = new ArrayList<>();
        if (credit == null) {
            return out;
        }
        for (final String part : credit.split("\\s*&\\s*")) {
            final String name = withoutNickname(part);
            if (!name.isEmpty()) {
                out.add(name);
            }
        }
        return out;
    }

    /** Wayne "King of" England is Wayne England; Lars Grant-"Wild Wild"-West is Lars Grant-West. */
    public static String withoutNickname(final String name) {
        return name.replaceAll("-\"[^\"]*\"-", "-").replaceAll("\\s*\"[^\"]*\"\\s*", " ").trim().replaceAll("\\s+", " ");
    }

    /** Whether the credit line names this artist, nicknames on either side ignored. */
    public static boolean credits(final String credit, final String artist) {
        final String wanted = withoutNickname(artist);
        for (final String a : individuals(credit)) {
            if (a.equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }
}
