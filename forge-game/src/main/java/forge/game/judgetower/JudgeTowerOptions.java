package forge.game.judgetower;

import java.util.Locale;

/**
 * The house rules a Judge Tower game is played under. The format has no central authority and its written sources
 * (mtg.wiki, TheGamer's guide, Cube Cobra's primer) disagree on these, so each is a choice; the defaults are the user's
 * rulings of 2026-10-30.
 */
public final class JudgeTowerOptions {
    /** Who owns a card out of the shared Tower. */
    public enum Ownership {
        /** mtg.wiki: the player who last played it. */
        LAST_PLAYED,
        /** TheGamer: the player who drew it. */
        DREW,
        /** Cube Cobra: the player who drew it, or put it onto their battlefield from the graveyard. */
        DREW_OR_REANIMATED
    }

    public enum LifeRule {
        /** TheGamer: tracked as usual, and 0 or less doesn't lose. */
        TRACKED,
        /** mtg.wiki: back to the starting total at the end of each turn, and 0 or less doesn't lose. */
        RESET,
        /** Cube Cobra: infinite. */
        INFINITE
    }

    public enum HiddenInfo {
        /** TheGamer: hands are revealed, the Tower isn't. */
        HANDS_REVEALED,
        /** Cube Cobra: hands, face-down exile and the top card of the Tower are all revealed. */
        ALL_PUBLIC,
        NORMAL
    }

    public Ownership ownership = Ownership.LAST_PLAYED;
    public LifeRule life = LifeRule.TRACKED;
    public HiddenInfo hidden = HiddenInfo.HANDS_REVEALED;
    public int pointsToWin = 3;
    public int xValue = 5;
    public int startingHand = 0;
    public boolean sharedGraveyard = true;
    /** How many infractions a player gets; the last one loses them the game (TheGamer's Three Strikes is 3). */
    public int strikes = 1;
    /** TheGamer: a card can't be played twice in a turn. Off by user ruling: a flashback is cast at once. */
    public boolean oneAndDone = false;

    public String serialize() {
        return "ownership=" + ownership + ";life=" + life + ";hidden=" + hidden + ";points=" + pointsToWin
                + ";x=" + xValue + ";hand=" + startingHand + ";sharedGraveyard=" + sharedGraveyard
                + ";strikes=" + strikes + ";oneAndDone=" + oneAndDone;
    }

    /** Anything missing or no longer understood keeps its default. */
    public static JudgeTowerOptions parse(final String text) {
        final JudgeTowerOptions o = new JudgeTowerOptions();
        if (text == null) {
            return o;
        }
        for (final String part : text.split(";")) {
            final String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            final String v = kv[1].trim();
            try {
                switch (kv[0].trim().toLowerCase(Locale.ROOT)) {
                    case "ownership" -> o.ownership = Ownership.valueOf(v);
                    case "life" -> o.life = LifeRule.valueOf(v);
                    case "hidden" -> o.hidden = HiddenInfo.valueOf(v);
                    case "points" -> o.pointsToWin = Math.max(1, Integer.parseInt(v));
                    case "x" -> o.xValue = Math.max(0, Integer.parseInt(v));
                    case "hand" -> o.startingHand = Math.max(0, Integer.parseInt(v));
                    case "sharedgraveyard" -> o.sharedGraveyard = Boolean.parseBoolean(v);
                    case "strikes" -> o.strikes = Math.max(1, Integer.parseInt(v));
                    case "oneanddone" -> o.oneAndDone = Boolean.parseBoolean(v);
                    default -> { }
                }
            } catch (final IllegalArgumentException ignored) {
                // an old or hand-edited preference
            }
        }
        return o;
    }
}
