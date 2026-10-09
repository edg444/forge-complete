package forge.game.keyword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.ICardFace;
import forge.item.PaperCard;

/**
 * Every keyword ability as it's been printed on a creature card, modifier and all (Modular Monstrosity: "a keyword ...
 * that's been printed on a creature card"; rulings: a modifier has to be one that's been printed - flying, annihilator 2,
 * protection from Demons). Read off the card database's creature faces, so it keeps up with new sets, and kept as the
 * script's keyword line, which is what the engine grants.
 */
public final class PrintedCreatureKeywords {
    private PrintedCreatureKeywords() { }

    // engine bookkeeping that happens to live in the keyword list, not abilities printed on cards
    private static final Set<Keyword> NOT_PRINTED = EnumSet.of(Keyword.UNDEFINED, Keyword.MAYFLASHCOST, Keyword.MAYFLASHSAC);

    private static List<String> all = null;

    /** The keyword lines, one per distinct printed form, in title order. */
    public static synchronized List<String> all() {
        if (all == null) {
            final Map<String, String> byTitle = new LinkedHashMap<>();
            for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
                final CardRules rules = pc.getRules();
                addFace(byTitle, rules.getMainPart());
                addFace(byTitle, rules.getOtherPart());
            }
            final List<String> lines = new ArrayList<>(byTitle.values());
            lines.sort(Comparator.comparing(PrintedCreatureKeywords::title, String.CASE_INSENSITIVE_ORDER));
            all = Collections.unmodifiableList(lines);
        }
        return all;
    }

    private static void addFace(final Map<String, String> byTitle, final ICardFace face) {
        if (face == null || face.getType() == null || !face.getType().isCreature() || face.getKeywords() == null) {
            return;
        }
        for (final String k : face.getKeywords()) {
            final KeywordInterface inst = Keyword.getInstance(k);
            if (NOT_PRINTED.contains(inst.getKeyword())) {
                continue;
            }
            final String title = inst.getTitle();
            if (title != null && !title.isEmpty()) {
                byTitle.putIfAbsent(title.toLowerCase(), k);
            }
        }
    }

    /** How a keyword line reads on a card: "Annihilator 2", "Protection from Demons". */
    public static String title(final String keywordLine) {
        return Keyword.getInstance(keywordLine).getTitle();
    }

    /** The keyword a line is a form of - what "a keyword you haven't chosen" counts, whatever the modifier. */
    public static Keyword keyword(final String keywordLine) {
        return Keyword.getInstance(keywordLine).getKeyword();
    }
}
