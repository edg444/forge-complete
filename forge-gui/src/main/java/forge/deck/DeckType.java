package forge.deck;

import forge.game.GameFormat;
import forge.model.FModel;
import forge.util.Localizer;

public enum DeckType {
    CUSTOM_DECK("lblCustomUserDecks"),
    CONSTRUCTED_DECK("lblConstructedDecks"),
    COMMANDER_DECK("lblCommanderDecks"),
    RANDOM_COMMANDER_DECK("lblRandomCommanderDecks"),
    RANDOM_CARDGEN_COMMANDER_DECK("lblRandomCommanderCard-basedDecks"),
    OATHBREAKER_DECK("lblOathbreakerDecks"),
    TINY_LEADERS_DECK("lblTinyLeadersDecks"),
    BRAWL_DECK("lblBrawlDecks"),
    SCHEME_DECK("lblSchemeDecks"),
    PLANAR_DECK("lblPlanarDecks"),
    DRAFT_DECK("lblDraftDecks"),
    SEALED_DECK("lblSealedDecks"),
    PRECONSTRUCTED_DECK("lblPreconstructedDecks"),
    PRECON_COMMANDER_DECK("lblPreconCommanderDecks"),
    QUEST_OPPONENT_DECK("lblQuestOpponentDecks"),
    COLOR_DECK("lblRandomColorDecks"),
    STANDARD_CARDGEN_DECK("lblRandomStandardArchetypeDecks"),
    PIONEER_CARDGEN_DECK("lblRandomPioneerArchetypeDecks"),
    HISTORIC_CARDGEN_DECK("lblRandomHistoricArchetypeDecks"),
    MODERN_CARDGEN_DECK("lblRandomModernArchetypeDecks"),
    LEGACY_CARDGEN_DECK("lblRandomLegacyArchetypeDecks"),
    VINTAGE_CARDGEN_DECK("lblRandomVintageArchetypeDecks"),
    PAUPER_CARDGEN_DECK("lblRandomPauperArchetypeDecks"),
    STANDARD_COLOR_DECK("lblRandomStandardColorDecks"),
    MODERN_COLOR_DECK("lblRandomModernColorDecks"),
    PAUPER_COLOR_DECK("lblRandomPauperColorDecks"),
    THEME_DECK("lblRandomThemeDecks"),
    RANDOM_DECK("lblRandomDecks"),
    NET_DECK("lblNetDecks"),
    NET_COMMANDER_DECK("lblNetCommanderDecks"),
    NET_ARCHIVE_STANDARD_DECK("lblNetArchiveStandardDecks"),
    NET_ARCHIVE_PIONEER_DECK("lblNetArchivePioneerDecks"),
    NET_ARCHIVE_MODERN_DECK("lblNetArchiveModernDecks"),
    NET_ARCHIVE_PAUPER_DECK("lblNetArchivePauperDecks"),
    NET_ARCHIVE_LEGACY_DECK("lblNetArchiveLegacyDecks"),
    NET_ARCHIVE_VINTAGE_DECK("lblNetArchiveVintageDecks"),
    NET_ARCHIVE_BLOCK_DECK("lblNetArchiveBlockDecks"),
    NET_ARCHIVE_COMMANDER_DECK("lblNetArchiveCommanderDecks"),
    NET_EVENT_DECK("lblNetEventDecks"),
    PROVIDED_DECK_URL("lblProvideDeckUrl");

    public static DeckType[] ConstructedOptions;
    public static DeckType[] CommanderOptions;

    static {
        if (FModel.isdeckGenMatrixLoaded()) {
            ConstructedOptions = new DeckType[]{
                    DeckType.CUSTOM_DECK,
                    DeckType.PRECONSTRUCTED_DECK,
                    DeckType.QUEST_OPPONENT_DECK,
                    DeckType.COLOR_DECK,
                    DeckType.STANDARD_CARDGEN_DECK,
                    DeckType.MODERN_CARDGEN_DECK,
                    DeckType.PAUPER_CARDGEN_DECK,
                    DeckType.LEGACY_CARDGEN_DECK,
                    DeckType.VINTAGE_CARDGEN_DECK,
                    DeckType.PIONEER_CARDGEN_DECK,
                    DeckType.HISTORIC_CARDGEN_DECK,
                    DeckType.THEME_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_DECK,
                    DeckType.NET_EVENT_DECK,
                    DeckType.NET_ARCHIVE_STANDARD_DECK,
                    DeckType.NET_ARCHIVE_PIONEER_DECK,
                    DeckType.NET_ARCHIVE_MODERN_DECK,
                    DeckType.NET_ARCHIVE_PAUPER_DECK,
                    DeckType.NET_ARCHIVE_LEGACY_DECK,
                    DeckType.NET_ARCHIVE_VINTAGE_DECK,
                    DeckType.NET_ARCHIVE_BLOCK_DECK,
                    DeckType.PROVIDED_DECK_URL
            };
        } else {
            ConstructedOptions = new DeckType[]{
                    DeckType.CUSTOM_DECK,
                    DeckType.PRECONSTRUCTED_DECK,
                    DeckType.QUEST_OPPONENT_DECK,
                    DeckType.COLOR_DECK,
                    DeckType.THEME_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_DECK,
                    DeckType.NET_EVENT_DECK,
                    DeckType.NET_ARCHIVE_STANDARD_DECK,
                    DeckType.NET_ARCHIVE_PIONEER_DECK,
                    DeckType.NET_ARCHIVE_MODERN_DECK,
                    DeckType.NET_ARCHIVE_PAUPER_DECK,
                    DeckType.NET_ARCHIVE_LEGACY_DECK,
                    DeckType.NET_ARCHIVE_VINTAGE_DECK,
                    DeckType.NET_ARCHIVE_BLOCK_DECK,
                    DeckType.PROVIDED_DECK_URL
            };
        }
    }
    static {
        if (FModel.isdeckGenMatrixLoaded()) {
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.PRECON_COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_CARDGEN_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK,
                    DeckType.PROVIDED_DECK_URL
            };
        }else{
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.PRECON_COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK,
                    DeckType.PROVIDED_DECK_URL
            };
        }

    }

    private String value;
    DeckType(final String value) {
        final Localizer localizer = Localizer.getInstance();
        this.value = localizer.getMessage(value);
    }

    /** The deck chooser entry for a saved deck type: the format color decks left the menu for the lobby's Format. */
    public DeckType forDeckChooser() {
        return switch (this) {
            case STANDARD_COLOR_DECK, MODERN_COLOR_DECK, PAUPER_COLOR_DECK -> COLOR_DECK;
            default -> this;
        };
    }

    /** The format an archetype list builds for, or null for any other deck type. */
    public GameFormat getArchetypeFormat() {
        return switch (this) {
            case STANDARD_CARDGEN_DECK -> FModel.getFormats().getStandard();
            case PIONEER_CARDGEN_DECK -> FModel.getFormats().getPioneer();
            case HISTORIC_CARDGEN_DECK -> FModel.getFormats().getHistoric();
            case MODERN_CARDGEN_DECK -> FModel.getFormats().getModern();
            case LEGACY_CARDGEN_DECK -> FModel.getFormats().getLegacy();
            case VINTAGE_CARDGEN_DECK -> FModel.getFormats().get("Vintage");
            case PAUPER_CARDGEN_DECK -> FModel.getFormats().getPauper();
            default -> null;
        };
    }

    /** What an archetype list of another format becomes under this format: its own list, or color decks if it has none. */
    public static DeckType archetypeListFor(final GameFormat format) {
        if (FModel.isdeckGenMatrixLoaded()) {
            for (final DeckType type : values()) {
                if (type.getArchetypeFormat() == format) {
                    return type;
                }
            }
        }
        return COLOR_DECK;
    }

    @Override
    public String toString() {
        return value;
    }
}
