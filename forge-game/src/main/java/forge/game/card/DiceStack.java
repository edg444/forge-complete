package forge.game.card;

import forge.util.MyRandom;

/**
 * Boomstacker's stack of real dice, simulated (user ruling): the odds of it toppling. Placing a die is gentle (a die
 * put down isn't part of the stack until it rests there untouched, rulings), turning the card sideways much less so
 * ("You must still tap Boomstacker... That's the fun part"). Both grow with the square of the height: a two-die stack
 * falls 2% of the time as a die is placed and 4% as the card turns, an eight-die stack 38% and 64%.
 */
public final class DiceStack {
    private DiceStack() { }

    private static final double PLACE = 0.006;
    private static final double TURN = 0.01;
    private static final double MOST = 0.95;

    public static double placeChance(final int newHeight) {
        return Math.min(MOST, PLACE * newHeight * newHeight);
    }

    public static double turnChance(final int height) {
        return Math.min(MOST, TURN * height * height);
    }

    /** Placing the die that would make the stack this tall knocks it over. */
    public static boolean fallsWhenPlacing(final int newHeight) {
        return newHeight > 1 && MyRandom.getRandom().nextDouble() < placeChance(newHeight);
    }

    public static boolean fallsWhenTurned(final int height) {
        return MyRandom.getRandom().nextDouble() < turnChance(height);
    }

    /** How many dice are still standing after a fall: at least one falls, maybe all, maybe just the top. */
    public static int diceLeftStanding(final int height) {
        return height <= 1 ? 0 : MyRandom.getRandom().nextInt(height);
    }
}
