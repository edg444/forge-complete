package forge.screens.home.conquest;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.deck.CardPool;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerContainer;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import net.miginfocom.swing.MigLayout;

/** Browse unlocked cards, exiling unwanted ones for aether shards or buying exiled ones back. */
public final class VSubmenuConquestCollection extends AConquestSubmenu {
    public static final VSubmenuConquestCollection INSTANCE = new VSubmenuConquestCollection();

    private final CardManager collectionList = new CardManager(null, false, false, false);
    private final CardManager exileList = new CardManager(null, false, false, false);
    private final JPanel pnlLists = new JPanel(new CardLayout());
    private final FButton btnCollection = new FButton();
    private final FButton btnExile = new FButton();
    private final FButton btnAction = new FButton();
    private final FLabel lblShards = new FLabel.Builder().fontSize(15).build();
    private final FLabel lblInfo = new FLabel.Builder().fontSize(12).fontAlign(SwingConstants.RIGHT).build();
    private final ConquestDesktop.CardPreview preview = new ConquestDesktop.CardPreview();
    private boolean showingExile;

    private VSubmenuConquestCollection() {
        super("lblCollection", true);
        pnlLists.setOpaque(false);
        pnlLists.add(new ItemManagerContainer(collectionList), "collection");
        pnlLists.add(new ItemManagerContainer(exileList), "exile");
        collectionList.addSelectionListener(e -> onSelectionChanged(collectionList));
        exileList.addSelectionListener(e -> onSelectionChanged(exileList));
        lblShards.setIcon(ConquestSkin.getIcon(FSkinProp.IMG_AETHER_SHARD, 24));
        btnCollection.setCommand(() -> showTab(false));
        btnExile.setCommand(() -> showTab(true));
        btnAction.setCommand(this::exileOrRetrieve);
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTCOLLECTION;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final JPanel top = new JPanel(new MigLayout("insets 0, gap 10"));
        top.setOpaque(false);
        top.add(btnCollection, "w 200px!, h 32px!");
        top.add(btnExile, "w 200px!, h 32px!");
        top.add(lblShards, "gapleft 20, h 32px!");
        top.add(lblInfo, "growx, pushx, h 32px!");

        final JPanel content = new JPanel(new MigLayout("insets 0, gap 10"));
        content.setOpaque(false);
        content.add(pnlLists, "grow, push");
        content.add(preview, "w 22%!, growy");

        display.add(top, "w 98%!, gap 1% 0 0 6px");
        display.add(content, "w 98%!, growy, pushy, gap 1% 0 0 10px");
        display.add(btnAction, "w 360px!, h 36px!, ax center, gapbottom 15px");

        collectionList.setup(ItemManagerConfig.CONQUEST_COLLECTION, ConquestDesktop.newCardColOverrides(ItemManagerConfig.CONQUEST_COLLECTION));
        exileList.setup(ItemManagerConfig.CONQUEST_COLLECTION, ConquestDesktop.newCardColOverrides(ItemManagerConfig.CONQUEST_COLLECTION));
        refreshCards();
        updateShards();
        showTab(showingExile);
    }

    private void refreshCards() {
        final ConquestData model = ConquestDesktop.model();
        final CardPool collection = new CardPool();
        final CardPool exile = new CardPool();
        collection.add(model.getUnlockedCards());
        collection.removeAllFlat(model.getExiledCards());
        exile.add(model.getExiledCards());
        collectionList.setPool(collection, true);
        exileList.setPool(exile, true);
        btnCollection.setText(localizer.getMessage("lblCollection") + " (" + collection.countDistinct() + ")");
        btnExile.setText(localizer.getMessage("lblExile") + " (" + exile.countDistinct() + ")");
    }

    private void updateShards() {
        final ConquestPreferences prefs = FModel.getConquestPreferences();
        final double baseValue = prefs.getPrefInt(CQPref.AETHER_BASE_DUPLICATE_VALUE);
        final double exileValue = prefs.getPrefInt(CQPref.AETHER_BASE_EXILE_VALUE);
        final double retrieveCost = prefs.getPrefInt(CQPref.AETHER_BASE_RETRIEVE_COST);
        lblShards.setText(localizer.getMessage("lblConquestShards", ConquestDesktop.model().getAEtherShards()));
        lblInfo.setText(localizer.getMessage("lblExileRetrieveProportion",
                Math.round(100 * exileValue / baseValue), Math.round(100 * retrieveCost / baseValue)));
    }

    private void showTab(final boolean exile) {
        showingExile = exile;
        ((CardLayout) pnlLists.getLayout()).show(pnlLists, exile ? "exile" : "collection");
        btnCollection.setEnabled(exile);
        btnExile.setEnabled(!exile);
        onSelectionChanged(exile ? exileList : collectionList);
    }

    private CardManager currentList() {
        return showingExile ? exileList : collectionList;
    }

    private void onSelectionChanged(final CardManager list) {
        if (list != currentList()) {
            return;
        }
        preview.setCard(list.getSelectedItem());
        final Collection<PaperCard> cards = list.getSelectedItems();
        final int total = totalValue(cards);
        btnAction.setText(localizer.getMessage(showingExile ? "lblRetrieveSelectedForNAE" : "lblExileSelectedForNAE", cards.size(), total));
        btnAction.setEnabled(!cards.isEmpty());
    }

    private int totalValue(final Collection<PaperCard> cards) {
        final CQPref pref = showingExile ? CQPref.AETHER_BASE_RETRIEVE_COST : CQPref.AETHER_BASE_EXILE_VALUE;
        int total = 0;
        for (final PaperCard card : cards) {
            total += ConquestUtil.getShardValue(card, pref);
        }
        return total;
    }

    private void exileOrRetrieve() {
        final List<PaperCard> cards = new ArrayList<>(currentList().getSelectedItems());
        if (cards.isEmpty()) {
            return;
        }
        final ConquestData model = ConquestDesktop.model();
        final int total = totalValue(cards);
        final boolean changed = showingExile ? model.retrieveCardsFromExile(cards, total) : model.exileCards(cards, total);
        if (changed) {
            refreshCards();
            updateShards();
            onSelectionChanged(currentList());
        }
    }
}
