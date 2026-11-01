package forge.deck;

import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.itemmanager.IItemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ColorDeckGenerator extends DeckProxy implements Comparable<ColorDeckGenerator> {
    /** @param format0 the format the decks must be legal in when they are generated, or null for none */
    public static List<DeckProxy> getColorDecks(final IItemManager<DeckProxy> lstDecks0, final Supplier<GameFormat> format0, final boolean isAi0) {
        final String[] colors = new String[] { "Random 1", "Random 2", "Random 3",
                "White", "Blue", "Black", "Red", "Green" };
        final List<DeckProxy> decks = new ArrayList<>();
        for (int i = 0; i < colors.length; i++) {
            decks.add(new ColorDeckGenerator(colors[i], i, lstDecks0, format0, isAi0));
        }
        return decks;
    }

    private final String name;
    private final int index;
    private final IItemManager<DeckProxy> lstDecks;
    private final boolean isAi;
    private final Supplier<GameFormat> format;

    private ColorDeckGenerator(final String name0, final int index0, final IItemManager<DeckProxy> lstDecks0, final Supplier<GameFormat> format0, final boolean isAi0) {
        super();
        name = name0;
        index = index0;
        lstDecks = lstDecks0;
        isAi = isAi0;
        format = format0;
    }

    public CardEdition getEdition() {
        return CardEdition.UNKNOWN;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final ColorDeckGenerator d) {
        return Integer.compare(index, d.index);
    }

    @Override
    public Deck getDeck() {
        final List<String> selection = new ArrayList<>();
        for (final DeckProxy deck : lstDecks.getSelectedItems()) {
            selection.add(deck.getName());
        }
        if (DeckgenUtil.colorCheck(selection)) {
            return DeckgenUtil.buildFormatColorDeck(selection, format.get(), isAi);
        }
        return null;
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}