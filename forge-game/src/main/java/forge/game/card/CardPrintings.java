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
}
