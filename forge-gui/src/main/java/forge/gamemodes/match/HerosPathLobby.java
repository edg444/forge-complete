package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.game.herospath.HerosPath;
import forge.game.herospath.HerosPath.Challenge;
import forge.game.herospath.HerosPath.Difficulty;
import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.MyRandom;

/**
 * The lobby's side of the Hero's Path challenges, shared by desktop and mobile: choosing a seat's Hero cards and their
 * Enhancement Stickers, and the challenge's difficulty. The choosers block, so mobile calls them off its UI thread.
 */
public final class HerosPathLobby {
    private HerosPathLobby() { }

    /** The playmats' difficulty options, asked as the match starts; the last choice is remembered. */
    public static Difficulty chooseDifficulty(final Challenge c) {
        final Localizer loc = Localizer.getInstance();
        final List<String> options = new ArrayList<>();
        for (final Difficulty d : Difficulty.values()) {
            options.add(difficultyLabel(c, d));
        }
        Difficulty last;
        try {
            last = Difficulty.valueOf(FModel.getPreferences().getPref(FPref.HEROS_PATH_DIFFICULTY));
        } catch (final IllegalArgumentException e) {
            last = Difficulty.NORMAL;
        }
        final String chosen = SGuiChoose.oneOrNone(loc.getMessage("lblHerosPathDifficulty", c.getName()), options,
                options.get(last.ordinal()), null);
        if (chosen == null) {
            return null;
        }
        final Difficulty d = Difficulty.values()[options.indexOf(chosen)];
        FModel.getPreferences().setPref(FPref.HEROS_PATH_DIFFICULTY, d.name());
        FModel.getPreferences().save();
        return d;
    }

    private static String difficultyLabel(final Challenge c, final Difficulty d) {
        final Localizer loc = Localizer.getInstance();
        final String level = loc.getMessage(d == Difficulty.EASY ? "lblHerosPathEasy" : d == Difficulty.HARD ? "lblHerosPathHard" : "lblHerosPathNormal");
        return switch (c) {
            case HYDRA -> loc.getMessage("lblHerosPathHydraLevel", level, c.startingCards(d, 1).size());
            case HORDE -> loc.getMessage("lblHerosPathHordeLevel", level, c.heroTurnsFirst(d));
            case GOD -> loc.getMessage("lblHerosPathGodLevel", level, c.startingCards(d, 1).size() - 1);
        };
    }

    /**
     * Choose a seat's Hero cards, one at a time up to the challenge's limit (Face the Hydra allows the same card twice),
     * then any Enhancement Stickers for each weapon. Returns null if cancelled.
     */
    public static List<String> chooseHeroCards(final Challenge c, final List<String> current) {
        final Localizer loc = Localizer.getInstance();
        final List<PaperCard> all = HerosPath.allHeroCards();
        final List<PaperCard> picked = new ArrayList<>();
        final String none = loc.getMessage("lblHerosPathNoMoreHeroes");
        while (picked.size() < c.maxHeroCards) {
            final List<Object> options = new ArrayList<>();
            options.add(none);
            for (final PaperCard pc : all) {
                if (c.duplicateHeroCards || !picked.contains(pc)) {
                    options.add(pc);
                }
            }
            final Object choice = SGuiChoose.oneOrNone(loc.getMessage("lblHerosPathChooseHero", picked.size() + 1, c.maxHeroCards),
                    options, null, o -> o instanceof PaperCard pc ? pc.getName() : String.valueOf(o));
            if (choice == null) {
                return picked.isEmpty() ? null : encode(picked, Collections.emptyList());
            }
            if (!(choice instanceof PaperCard pc)) {
                break;
            }
            picked.add(pc);
        }
        final List<String> descriptions = HerosPath.enhancementDescriptions();
        final List<List<Integer>> stickers = new ArrayList<>();
        for (final PaperCard pc : picked) {
            final List<Integer> nums = new ArrayList<>();
            if (HerosPath.takesEnhancements(pc) && !descriptions.isEmpty()) {
                final List<String> chosen = SGuiChoose.getChoices(loc.getMessage("lblHerosPathChooseStickers", pc.getName()),
                        0, descriptions.size(), descriptions);
                if (chosen != null) {
                    for (final String d : chosen) {
                        nums.add(descriptions.indexOf(d) + 1);
                    }
                    Collections.sort(nums);
                }
            }
            stickers.add(nums);
        }
        return encode(picked, stickers);
    }

    private static List<String> encode(final List<PaperCard> picked, final List<List<Integer>> stickers) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            result.add(HerosPath.HeroChoice.encode(picked.get(i).getName(), i < stickers.size() ? stickers.get(i) : null));
        }
        return result;
    }

    /** For a seat's panel: "The Slayer, Spear of the General (2 stickers)", or "None". */
    public static String describe(final List<String> heroCards) {
        if (heroCards == null || heroCards.isEmpty()) {
            return Localizer.getInstance().getMessage("lblNone");
        }
        final List<String> parts = new ArrayList<>();
        for (final HerosPath.HeroChoice hc : HerosPath.parseHeroChoices(heroCards)) {
            final int n = hc.enhancements().size();
            parts.add(n == 0 ? hc.card().getName() : hc.card().getName() + " (+" + n + ")");
        }
        return String.join(", ", parts);
    }

    /** An AI seat with no Hero cards chosen brings a random legal set, weapons without stickers. */
    public static List<String> randomHeroCards(final Challenge c) {
        final List<PaperCard> all = new ArrayList<>(HerosPath.allHeroCards());
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < c.maxHeroCards && !all.isEmpty(); i++) {
            final PaperCard pc = all.get(MyRandom.getRandom().nextInt(all.size()));
            if (!c.duplicateHeroCards) {
                all.remove(pc);
            }
            result.add(pc.getName());
        }
        return result;
    }
}
