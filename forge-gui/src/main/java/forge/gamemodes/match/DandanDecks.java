package forge.gamemodes.match;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Dandan (Forgetful Fish): the shared decks it can be played with, from res/dandan in file name order (the Secret Lair
 * list first). The lobby's choice is FPref.DANDAN_DECK, by deck name.
 */
public final class DandanDecks {
    private DandanDecks() { }

    private static List<File> files() {
        final File[] found = new File(ForgeConstants.DANDAN_DIR).listFiles((dir, name) -> name.endsWith(".dck"));
        if (found == null) {
            return List.of();
        }
        Arrays.sort(found);
        return Arrays.asList(found);
    }

    public static List<String> names() {
        final List<String> names = new ArrayList<>();
        for (final File f : files()) {
            names.add(DeckSerializer.fromFile(f).getName());
        }
        return names;
    }

    /** The chosen deck's name, or the first one's when the choice isn't one of them. */
    public static String selectedName() {
        final List<String> names = names();
        final String chosen = FModel.getPreferences().getPref(FPref.DANDAN_DECK);
        return names.contains(chosen) || names.isEmpty() ? chosen : names.get(0);
    }

    public static void select(final String name) {
        FModel.getPreferences().setPref(FPref.DANDAN_DECK, name);
        FModel.getPreferences().save();
    }

    /** A fresh copy of the chosen deck, so a game can't change the stored one. */
    public static Deck selected() {
        final String name = selectedName();
        for (final File f : files()) {
            final Deck d = DeckSerializer.fromFile(f);
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return new Deck();
    }
}
