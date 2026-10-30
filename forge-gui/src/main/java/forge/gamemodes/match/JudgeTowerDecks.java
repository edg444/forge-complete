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
import forge.util.Localizer;

/**
 * Judge Tower: the shared deck (the Tower) it's played with, either one from res/judgetower or the deck picked for the
 * first seat, so that a group can bring its own. The lobby's choice is FPref.JUDGE_TOWER_DECK.
 */
public final class JudgeTowerDecks {
    private JudgeTowerDecks() { }

    private static List<File> files() {
        final File[] found = new File(ForgeConstants.JUDGE_TOWER_DIR).listFiles((dir, name) -> name.endsWith(".dck"));
        if (found == null) {
            return List.of();
        }
        Arrays.sort(found);
        return Arrays.asList(found);
    }

    private static String seatDeckName() {
        return Localizer.getInstance().getMessage("lblJudgeTowerSeatDeck");
    }

    public static List<String> names() {
        final List<String> names = new ArrayList<>();
        for (final File f : files()) {
            names.add(DeckSerializer.fromFile(f).getName());
        }
        names.add(seatDeckName());
        return names;
    }

    /** The chosen deck's name, or the first one's when the choice isn't one of them. */
    public static String selectedName() {
        final List<String> names = names();
        final String chosen = FModel.getPreferences().getPref(FPref.JUDGE_TOWER_DECK);
        return names.contains(chosen) ? chosen : names.get(0);
    }

    public static void select(final String name) {
        FModel.getPreferences().setPref(FPref.JUDGE_TOWER_DECK, name);
        FModel.getPreferences().save();
    }

    /** A fresh copy of the chosen deck, so a game can't change the stored one; null if the first seat has none. */
    public static Deck selected(final Deck firstSeatDeck) {
        final String name = selectedName();
        for (final File f : files()) {
            final Deck d = DeckSerializer.fromFile(f);
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return firstSeatDeck;
    }
}
