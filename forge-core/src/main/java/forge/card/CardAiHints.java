package forge.card;

/** 
 * CardAiHints holds all the different types of card hints for AI decks.
 *
 */
public class CardAiHints {

    private final boolean isRemovedFromAIDecks;
    private final boolean isRemovedFromRandomDecks;
    private final boolean isRemovedFromNonCommanderDecks;
    private final boolean isRemovedFromNoSideboardDecks;

    private final DeckHints deckHints;
    private final DeckHints deckNeeds;
    private final DeckHints deckHas;

    public CardAiHints(boolean remAi, boolean remRandom, boolean remUnlessCommander, DeckHints dh, DeckHints dn, DeckHints has) {
        this(remAi, remRandom, remUnlessCommander, false, dh, dn, has);
    }

    public CardAiHints(boolean remAi, boolean remRandom, boolean remUnlessCommander, boolean remNoSideboard, DeckHints dh,
            DeckHints dn, DeckHints has) {
        isRemovedFromNoSideboardDecks = remNoSideboard;
        isRemovedFromAIDecks = remAi;
        isRemovedFromRandomDecks = remRandom;
        isRemovedFromNonCommanderDecks = remUnlessCommander;
        deckHints = dh;
        deckNeeds = dn;
        deckHas = has;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return this.isRemovedFromAIDecks;
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    /**
     * AI:RemoveDeck:NoSideboard - a card that only works on sideboards (Jester's Sombrero) stays out of generated
     * decks for games without them: random constructed, Commander and themed decks. Limited decks have a sideboard
     * (the rest of the pool), so they keep it.
     */
    public boolean getRemNoSideboardDecks() {
        return isRemovedFromNoSideboardDecks;
    }

    public boolean getRemRandomDecks() {
        return this.isRemovedFromRandomDecks;
    }

    /**
     * Gets the rem random decks.
     *
     * @return the rem random decks
     */
    public boolean getRemNonCommanderDecks() {
        return this.isRemovedFromNonCommanderDecks;
    }

        /**
         * @return the deckHints
         */
    public DeckHints getDeckHints() {
        return deckHints;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckNeeds() {
        return deckNeeds;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckHas() {
        return deckHas;
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (this.isRemovedFromAIDecks && this.isRemovedFromRandomDecks) {
            return 3;
        } else if (this.isRemovedFromAIDecks) {
            return 4;
        } else if (this.isRemovedFromRandomDecks) {
            return 2;
        } else {
            return 1;
        }
    }

}
