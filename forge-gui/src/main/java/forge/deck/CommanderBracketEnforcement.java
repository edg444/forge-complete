package forge.deck;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

/**
 * The Commander bracket rule a table can enforce at match start, separate from deck conformance: off, every deck at
 * or under a bracket, or every deck in the same bracket. Brackets come from {@link CommanderBracketCalculator}, which
 * finds the lowest bracket a deck's Game Changers, mass land denial, extra turns and two-card combos allow, so a deck
 * built for a lower bracket than its cards allow still reads as the bracket its cards require.
 */
public final class CommanderBracketEnforcement {
    public static final String OFF = "Off";
    public static final String MAX_PREFIX = "Max ";
    public static final String ALL_MATCH = "All Match";
    /** The choices, in the order the settings show them (Max 5 would be no limit, so it isn't one). */
    public static final String[] CHOICES = {OFF, MAX_PREFIX + 1, MAX_PREFIX + 2, MAX_PREFIX + 3, MAX_PREFIX + 4, ALL_MATCH};

    private static final String GENERATED_PREFIX = "Generated ";
    private static final int REGENERATION_TRIES = 6;

    public enum Mode { OFF, MAX, ALL_MATCH }

    /** One setting's worth of rule: the mode, and for {@link Mode#MAX} the highest bracket allowed. */
    public record Rule(Mode mode, int maxBracket) {
        public static Rule parse(final String setting) {
            if (setting != null && setting.startsWith(MAX_PREFIX)) {
                try {
                    final int n = Integer.parseInt(setting.substring(MAX_PREFIX.length()).trim());
                    if (n >= 1 && n <= 4) {
                        return new Rule(Mode.MAX, n);
                    }
                } catch (final NumberFormatException ignored) {
                    // an unreadable setting is no rule
                }
            }
            if (ALL_MATCH.equals(setting)) {
                return new Rule(Mode.ALL_MATCH, 5);
            }
            return new Rule(Mode.OFF, 5);
        }

        public boolean isOff() {
            return mode == Mode.OFF;
        }
    }

    private CommanderBracketEnforcement() {
    }

    public static Rule currentRule() {
        return Rule.parse(FModel.getPreferences().getPref(FPref.ENFORCE_COMMANDER_BRACKET));
    }

    /** The highest bracket a generated Commander deck may have: the deck generator's own maximum, lowered to a bracket cap. */
    public static int generationCap(final int deckgenMaximum) {
        final Rule rule = currentRule();
        return rule.mode() == Mode.MAX ? Math.min(deckgenMaximum, rule.maxBracket()) : deckgenMaximum;
    }

    /** Whether a deck came from the random Commander deck generator, which can make it again to fit. */
    public static boolean isGenerated(final Deck deck) {
        return deck != null && deck.getName() != null && deck.getName().startsWith(GENERATED_PREFIX)
                && deck.getCommanders() != null && !deck.getCommanders().isEmpty();
    }

    /**
     * The bracket every deck should share under {@link Mode#ALL_MATCH}: the one the decks that can't be made again
     * agree on, or null when they disagree or there are none.
     */
    public static Integer sharedTarget(final Iterable<Deck> fixedDecks) {
        final TreeSet<Integer> brackets = new TreeSet<>();
        for (final Deck deck : fixedDecks) {
            brackets.add(CommanderBracketCalculator.getBracket(deck));
        }
        return brackets.size() == 1 ? brackets.first() : null;
    }

    /** Whether a deck's bracket fits the rule, for {@link Mode#ALL_MATCH} against the given target. */
    public static boolean fits(final Rule rule, final int bracket, final Integer target) {
        return switch (rule.mode()) {
            case OFF -> true;
            case MAX -> bracket <= rule.maxBracket();
            case ALL_MATCH -> target == null || bracket == target;
        };
    }

    /**
     * Makes a generated deck again, with the same commander, until it fits: for {@link Mode#MAX} at or under the cap,
     * for {@link Mode#ALL_MATCH} exactly the target (the generator only caps, so failing that, the closest under it).
     * Returns the deck unchanged when it already fits or no attempt does better.
     */
    public static Deck refitGenerated(final Deck deck, final DeckFormat format, final Rule rule, final Integer target) {
        final int bracket = CommanderBracketCalculator.getBracket(deck);
        if (!isGenerated(deck) || fits(rule, bracket, target) || (rule.mode() == Mode.ALL_MATCH && target == null)) {
            return deck;
        }
        final int cap = rule.mode() == Mode.MAX ? rule.maxBracket() : target;
        final PaperCard commander = deck.getCommanders().get(0);
        Deck best = deck;
        int bestBracket = bracket;
        for (int i = 0; i < REGENERATION_TRIES; i++) {
            final Deck attempt = DeckgenUtil.generateRandomCommanderDeck(commander, format, true, false, cap);
            final int b = CommanderBracketCalculator.getBracket(attempt);
            if (fits(rule, b, target)) {
                return attempt;
            }
            // nearer the target from below beats anything over it
            if (b <= cap && (bestBracket > cap || b > bestBracket)) {
                best = attempt;
                bestBracket = b;
            }
        }
        return best;
    }

    /**
     * One line per deck that breaks the rule, naming its bracket and what puts it there; empty when every deck fits
     * or the rule is off.
     */
    public static List<String> problems(final Rule rule, final Map<String, Deck> decksByPlayer) {
        final List<String> problems = new ArrayList<>();
        if (rule.isOff() || decksByPlayer.isEmpty()) {
            return problems;
        }
        final Localizer localizer = Localizer.getInstance();
        final Map<String, Integer> brackets = new LinkedHashMap<>();
        for (final Map.Entry<String, Deck> e : decksByPlayer.entrySet()) {
            brackets.put(e.getKey(), CommanderBracketCalculator.getBracket(e.getValue()));
        }
        if (rule.mode() == Mode.MAX) {
            for (final Map.Entry<String, Integer> e : brackets.entrySet()) {
                if (e.getValue() > rule.maxBracket()) {
                    problems.add(localizer.getMessage("lblCommanderBracketOverMax", e.getKey(), e.getValue(), rule.maxBracket())
                            + reasons(decksByPlayer.get(e.getKey())));
                }
            }
        } else if (new TreeSet<>(brackets.values()).size() > 1) {
            for (final Map.Entry<String, Integer> e : brackets.entrySet()) {
                problems.add(localizer.getMessage("lblCommanderBracketDeckIs", e.getKey(), e.getValue())
                        + reasons(decksByPlayer.get(e.getKey())));
            }
        }
        return problems;
    }

    /** What raises a deck above bracket 1, indented under its line. */
    private static String reasons(final Deck deck) {
        final CommanderBracketCalculator.Result result = CommanderBracketCalculator.calculate(deck);
        final List<String> found = result.getReasonCards();
        return found.isEmpty() ? "" : "\n    " + String.join(", ", found);
    }
}
