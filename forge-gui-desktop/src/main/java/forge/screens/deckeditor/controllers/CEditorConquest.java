package forge.screens.deckeditor.controllers;

import java.util.List;
import java.util.Map.Entry;

import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.AddBasicLandsDialog;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VBrawlDecks;
import forge.screens.deckeditor.views.VCommanderDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.deckeditor.views.VOathbreakerDecks;
import forge.screens.deckeditor.views.VTinyLeadersDecks;
import forge.screens.home.conquest.ConquestDesktop;
import forge.screens.match.controllers.CDetailPicture;

/**
 * Edits a Planar Conquest commander's deck. The catalog is the conquest's unlocked, unexiled cards;
 * the deck is stored under the commander's name, so it can't be renamed, replaced or saved elsewhere.
 */
public final class CEditorConquest extends CDeckEditor<Deck> {
    private final ConquestCommander commander;
    private final DeckController<Deck> controller;
    private DragCell allDecksParent;
    private DragCell commanderDecksParent;
    private DragCell oathbreakerDecksParent;
    private DragCell brawlDecksParent;
    private DragCell tinyLeadersDecksParent;
    private DragCell deckGenParent;

    public CEditorConquest(final ConquestCommander commander0, final CDetailPicture cDetailPicture0) {
        super(FScreen.DECK_EDITOR_CONQUEST, cDetailPicture0, GameType.PlanarConquest);
        commander = commander0;

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false, false);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false, false);
        catalogManager.setCaption(localizer.getMessage("lblCollection"));
        setCatalogManager(catalogManager);
        setDeckManager(deckManager);

        controller = new DeckController<>(FModel.getConquest().getDecks(), this, Deck::new);
        getBtnAddBasicLands().setCommand((UiCommand) this::addBasicLands);
    }

    private void addBasicLands() {
        final Deck deck = controller.getModel();
        if (deck == null) {
            return;
        }
        final List<CardEdition> sets = ConquestUtil.getBasicLandSets(deck);
        final AddBasicLandsDialog dialog = sets.isEmpty() ? new AddBasicLandsDialog(deck) : new AddBasicLandsDialog(deck, sets.get(0));
        final CardPool landsToAdd = dialog.show();
        if (landsToAdd != null) {
            onAddItems(landsToAdd, false);
        }
    }

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.Singleton;
    }

    @Override
    protected void onAddItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        if (!toAlternate) {
            CEditorConstructed.onAddItems(this, items, false);
        }
    }

    @Override
    protected void onRemoveItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        if (!toAlternate) {
            CEditorConstructed.onRemoveItems(this, items, false);
        }
    }

    @Override
    protected void buildAddContextMenu(final EditorContextMenuBuilder cmb) {
        cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltodeck"));
    }

    @Override
    protected void buildRemoveContextMenu(final EditorContextMenuBuilder cmb) {
        cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromdeck"));
    }

    @Override
    public void resetTables() {
        final Deck deck = controller.getModel();
        final CardPool catalog = ConquestUtil.getAvailablePool();
        catalog.removeAllFlat(deck.getMain().toFlatList());
        if (deck.has(DeckSection.Commander)) {
            catalog.removeAllFlat(deck.get(DeckSection.Commander).toFlatList());
        }
        getCatalogManager().setPool(catalog);
        getDeckManager().setPool(deck.getMain());
    }

    @Override
    public DeckController<Deck> getDeckController() {
        return controller;
    }

    @Override
    public void update() {
        getCatalogManager().setup(ItemManagerConfig.CONQUEST_COLLECTION, ConquestDesktop.newCardColOverrides(ItemManagerConfig.CONQUEST_COLLECTION));
        getDeckManager().setup(ItemManagerConfig.CONQUEST_DECK_EDITOR, ConquestDesktop.newCardColOverrides(ItemManagerConfig.CONQUEST_DECK_EDITOR));

        resetUI();
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(false);
        getCbxSection().setVisible(false);

        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        commanderDecksParent = removeTab(VCommanderDecks.SINGLETON_INSTANCE);
        oathbreakerDecksParent = removeTab(VOathbreakerDecks.SINGLETON_INSTANCE);
        brawlDecksParent = removeTab(VBrawlDecks.SINGLETON_INSTANCE);
        tinyLeadersDecksParent = removeTab(VTinyLeadersDecks.SINGLETON_INSTANCE);
        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);

        // makes sure the deck exists in storage before loading it by name
        commander.getDeck();
        controller.load(null, commander.getName());
    }

    @Override
    public boolean canSwitchAway(final boolean isClosing) {
        if (!SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_CONQUEST, isClosing)) {
            return false;
        }
        commander.reloadDeck();
        return true;
    }

    @Override
    public void resetUIChanges() {
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(true);
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
            allDecksParent = null;
        }
        if (commanderDecksParent != null) {
            commanderDecksParent.addDoc(VCommanderDecks.SINGLETON_INSTANCE);
            commanderDecksParent = null;
        }
        if (oathbreakerDecksParent != null) {
            oathbreakerDecksParent.addDoc(VOathbreakerDecks.SINGLETON_INSTANCE);
            oathbreakerDecksParent = null;
        }
        if (brawlDecksParent != null) {
            brawlDecksParent.addDoc(VBrawlDecks.SINGLETON_INSTANCE);
            brawlDecksParent = null;
        }
        if (tinyLeadersDecksParent != null) {
            tinyLeadersDecksParent.addDoc(VTinyLeadersDecks.SINGLETON_INSTANCE);
            tinyLeadersDecksParent = null;
        }
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
            deckGenParent = null;
        }
    }
}
