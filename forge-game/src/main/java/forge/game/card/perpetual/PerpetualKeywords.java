package forge.game.card.perpetual;

import java.util.Collections;
import java.util.List;

import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;

public class PerpetualKeywords implements PerpetualInterface {
    private final long timestamp;
    private final List<String> addKeywords;
    private final List<String> removeKeywords;
    private final boolean removeAll;

    // Cache of the KeywordInterface objects actually derived (via Keyword.createTraits) the first
    // time this grant is applied. Card.setPerpetual(oldCard) re-invokes applyEffect every time this
    // same logical card ends up represented by a new Card object across a zone change - without
    // reusing the already-derived objects here, each reapplication built brand new KeywordInterfaces
    // from scratch, and for a keyword that expands into a derived trigger (e.g. Evoke's "sacrifice it"
    // trigger), that meant re-deriving and re-adding a duplicate trigger on every reapplication.
    private List<KeywordInterface> cachedKeywords;

    public PerpetualKeywords(long timestamp, List<String> addKeywords, List<String> removeKeywords, boolean removeAll) {
        this(timestamp, addKeywords, removeKeywords, removeAll, null);
    }

    // Callers that already derived the KeywordInterface list themselves (e.g. doAnimate applying the
    // grant directly to the card at the same moment it constructs this wrapper) should pass it here,
    // so that wrapper's own first applyEffect call reuses it instead of deriving a second, independent
    // copy - which is what caused a keyword that expands into a trigger (e.g. Evoke) to end up granted
    // twice from a single cast.
    public PerpetualKeywords(long timestamp, List<String> addKeywords, List<String> removeKeywords, boolean removeAll,
            List<KeywordInterface> alreadyAppliedKeywords) {
        this.timestamp = timestamp;
        this.addKeywords = addKeywords;
        this.removeKeywords = removeKeywords;
        this.removeAll = removeAll;
        this.cachedKeywords = alreadyAppliedKeywords;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public List<String> addKeywords() {
        return addKeywords;
    }

    public List<String> removeKeywords() {
        return removeKeywords;
    }

    public boolean removeAll() {
        return removeAll;
    }

    @Override
    public void applyEffect(Card c) {
        if (cachedKeywords == null) {
            cachedKeywords = c.addChangedCardKeywords(addKeywords, removeKeywords, removeAll, timestamp, null);
        } else {
            c.addChangedCardKeywordsInternal(cachedKeywords, Collections.emptyList(), removeAll, timestamp, null, true);
        }
    }
}
