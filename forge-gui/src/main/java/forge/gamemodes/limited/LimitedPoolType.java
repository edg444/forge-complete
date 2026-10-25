package forge.gamemodes.limited;

import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.item.SealedTemplate;
import forge.item.generation.IUnOpenedProduct;
import forge.item.generation.UnOpenedProduct;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

import java.util.function.Predicate;

public enum LimitedPoolType {
    Full(Localizer.getInstance().getMessage("lblLimitedPoolFull")),
    Block(Localizer.getInstance().getMessage("lblLimitedBlock")),
    Prerelease(Localizer.getInstance().getMessage("lblLimitedPrerelease"), false),
    FantasyBlock(Localizer.getInstance().getMessage("lblLimitedFantasy")),
    Custom(Localizer.getInstance().getMessage("lblLimitedCustom")),
    Chaos(Localizer.getInstance().getMessage("lblLimitedChaos")),
    Import(Localizer.getInstance().getMessage("lblLimitedImportFromUrl"));

    private final String displayName;
    private final boolean draftable;

    LimitedPoolType(String name) {
        this(name, true);
    }

    LimitedPoolType(String name, boolean draftable) {
        this.draftable = draftable;
        displayName = name;
    }

    public static LimitedPoolType[] values(boolean draftable) {
        if (!draftable) {
            return values();
        }

        int n = 0;
        for (LimitedPoolType lpt : values()) {
            if (lpt.draftable) {
                n++;
            }
        }
        LimitedPoolType[] draftableFormats = new LimitedPoolType[n];
        n = 0;
        for (LimitedPoolType lpt : values()) {
            if (lpt.draftable) {
                draftableFormats[n] = lpt;
                n++;
            }
        }
        return draftableFormats;
    }

    /**
     * Packs for a Full card pool draft or sealed: every printing, less The List's and the Un-cards when the
     * preferences say so.
     */
    public static IUnOpenedProduct fullCardpoolBooster() {
        Predicate<PaperCard> filter = null;
        if (FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_THE_LIST)) {
            filter = PaperCardPredicates.NOT_ON_THE_LIST;
        }
        if (FModel.getPreferences().getPrefBoolean(FPref.EXCLUDE_FUNNY_CARDS)) {
            filter = filter == null ? PaperCardPredicates.NOT_FUNNY : filter.and(PaperCardPredicates.NOT_FUNNY);
        }
        if (filter != null) {
            return new UnOpenedProduct(SealedTemplate.genericDraftBooster, filter);
        }
        return new UnOpenedProduct(SealedTemplate.genericDraftBooster);
    }

    @Override
    public String toString() {
        return displayName;
    }
}