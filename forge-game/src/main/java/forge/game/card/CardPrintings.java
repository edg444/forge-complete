package forge.game.card;

import java.util.Calendar;
import java.util.Date;

import forge.StaticData;
import forge.card.CardEdition;
import forge.item.PaperCard;

/**
 * When a card was printed. Delphia, Undecided asks where a card was originally printed; Gen, Confider of Ages and The
 * Generational Historian ask the year a card was released, which is its own printing's ("specific editions matter").
 */
public final class CardPrintings {
    private CardPrintings() { }

    /**
     * The set this card's name was first printed in, by set release date; null if Forge has no printing of it. Promo
     * sets don't count: catch-all ones like DCI Promos and Magic Online Promos carry one early date for years of cards.
     */
    public static CardEdition originalEdition(final Card c) {
        CardEdition first = null;
        for (final PaperCard pc : StaticData.instance().getCommonCards().getAllCards(c.getName())) {
            final CardEdition ed = StaticData.instance().getEditions().get(pc.getEdition());
            if (ed == null || ed.getType() == CardEdition.Type.PROMO || ed.getName().endsWith("Promos")) {
                continue;
            }
            if (ed.getDate() != null && (first == null || ed.getDate().before(first.getDate()))) {
                first = ed;
            }
        }
        return first;
    }

    public static boolean originallyPrintedIn(final Card c, final java.util.Collection<String> setCodes) {
        final CardEdition first = originalEdition(c);
        return first != null && setCodes.contains(first.getCode());
    }

    /**
     * Lona, Tracker of the Known: the different English releases the card's name has been printed in - every set Forge
     * has it in, digital-only ones included, but not promos, custom sets, or sets in another language (user ruling).
     */
    public static int releaseCount(final Card c) {
        final java.util.Set<String> sets = new java.util.HashSet<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getAllCards(c.getName())) {
            final CardEdition ed = StaticData.instance().getEditions().get(pc.getEdition());
            if (ed == null || ed.getType() == CardEdition.Type.PROMO || ed.getType() == CardEdition.Type.CUSTOM_SET) {
                continue;
            }
            final String name = ed.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("chinese") || name.contains("japanese") || name.contains("korean") || name.contains("foreign")) {
                continue;
            }
            sets.add(ed.getCode());
        }
        return sets.size();
    }

    /** The year this card's printing was released, or -1 if it isn't known (tokens, cards without a set). */
    public static int releaseYear(final Card c) {
        final String code = c.getSetCode();
        final CardEdition ed = code == null ? null : StaticData.instance().getEditions().get(code);
        final Date date = ed == null ? null : ed.getDate();
        if (date == null) {
            return -1;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    // Sixth Stage of Magic Design (user ruling 2026-10-22): Alpha through Fifth Edition have no expansion symbol, and
    // Dominaria and Coldsnap are the only sets whose commons' symbols are white
    private static final java.util.Set<String> NO_SYMBOL = java.util.Set.of("LEA", "LEB", "2ED", "3ED", "4ED", "5ED",
            "CED", "CEI", "FBB", "SUM", "4BB");
    private static final java.util.Set<String> WHITE_COMMONS = java.util.Set.of("DOM", "CSP");

    /**
     * Whether this printing's expansion symbol is black or silver: every set before Exodus, which brought in rarity
     * colors, is black; after it, commons (and basic lands) are black and uncommons silver. Tokens count as black.
     */
    public static boolean hasBlackOrSilverSymbol(final Card c) {
        final String code = c.getSetCode();
        if (code == null || code.isEmpty() || NO_SYMBOL.contains(code)) {
            return false;
        }
        if (c.isToken()) {
            return true;
        }
        final CardEdition ed = StaticData.instance().getEditions().get(code);
        final CardEdition exodus = StaticData.instance().getEditions().get("EXO");
        if (ed != null && exodus != null && ed.getDate() != null && ed.getDate().before(exodus.getDate())) {
            return true;
        }
        switch (c.getRarity()) {
            case Uncommon:
                return true;
            case Common:
            case BasicLand:
                return !WHITE_COMMONS.contains(code);
            default:
                return false;
        }
    }
}
