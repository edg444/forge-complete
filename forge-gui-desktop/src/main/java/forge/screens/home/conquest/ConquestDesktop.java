package forge.screens.home.conquest;

import java.util.HashMap;
import java.util.Map;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.views.ItemTableColumn;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JPanel;

import forge.ImageCache;
import forge.Singletons;
import forge.game.card.CardView;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorConquest;
import forge.screens.home.CHomeUI;
import forge.toolbox.FSkin;
import forge.toolbox.special.CardZoomer;

/** Helpers shared by the desktop Planar Conquest screens. */
public final class ConquestDesktop {
    private ConquestDesktop() {
    }

    public static ConquestData model() {
        return FModel.getConquest().getModel();
    }

    /** Loads the last-played conquest from disk if none is active yet; false when there's none to load. */
    public static boolean ensureConquest() {
        if (model() != null) {
            return true;
        }
        final String name = FModel.getConquestPreferences().getPref(CQPref.CURRENT_CONQUEST);
        final File dir = new File(ForgeConstants.CONQUEST_SAVE_DIR, name.replace(' ', '_'));
        if (!new File(dir, "data.xml").isFile()) {
            return false;
        }
        final ConquestData data = new ConquestData(dir);
        if (data.getCurrentLocation() == null || data.getPlaneswalker() == null) {
            return false;
        }
        FModel.getConquest().setModel(data);
        return true;
    }

    public static void open(final EDocID doc) {
        if (Singletons.getControl().getCurrentScreen() != FScreen.HOME_SCREEN) {
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
        }
        CHomeUI.SINGLETON_INSTANCE.itemClick(doc);
    }

    public static void zoom(final PaperCard card) {
        if (card == null) {
            return;
        }
        final CardView view = CardView.getCardForUi(card);
        if (view == null) {
            return;
        }
        CardZoomer.SINGLETON_INSTANCE.setCard(view.getCurrentState(), true);
        CardZoomer.SINGLETON_INSTANCE.doMouseWheelZoom();
    }

    public static void editDeck(final ConquestCommander commander) {
        if (!Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_CONQUEST)) {
            return;
        }
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                new CEditorConquest(commander, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
    }

    /** Table column overrides that mark cards unlocked since the conquest's new-card list was last cleared. */
    public static Map<ColumnDef, ItemTableColumn> newCardColOverrides(final ItemManagerConfig config) {
        final Map<ColumnDef, ItemTableColumn> overrides = new HashMap<>();
        ItemTableColumn.addColOverride(config, overrides, ColumnDef.NEW, ConquestData.getFnNewCompare(), ConquestData.getFnNewGet());
        return overrides;
    }

    public static Image toImage(final ISkinImage skinImage) {
        if (skinImage instanceof FSkin.SkinImage si && si.getIcon() != null) {
            return si.getIcon().getImage();
        }
        return null;
    }

    /** Draws an image scaled to fill the box while keeping its aspect ratio (cropping the overflow). */
    public static void drawCover(final Graphics2D g, final Image image, final int x, final int y, final int w, final int h) {
        final int iw = image.getWidth(null);
        final int ih = image.getHeight(null);
        if (iw <= 0 || ih <= 0) {
            return;
        }
        final double scale = Math.max((double) w / iw, (double) h / ih);
        final int dw = (int) Math.ceil(iw * scale);
        final int dh = (int) Math.ceil(ih * scale);
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.clipRect(x, y, w, h);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        g2.dispose();
    }

    /** Draws an image as large as fits in the box, centered, keeping its aspect ratio. */
    public static void drawContain(final Graphics2D g, final Image image, final int x, final int y, final int w, final int h) {
        final int iw = image.getWidth(null);
        final int ih = image.getHeight(null);
        if (iw <= 0 || ih <= 0) {
            return;
        }
        final double scale = Math.min((double) w / iw, (double) h / ih);
        final int dw = (int) Math.round(iw * scale);
        final int dh = (int) Math.round(ih * scale);
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        g2.dispose();
    }

    /** A panel showing one card's full image (or an arbitrary image) as large as fits. */
    @SuppressWarnings("serial")
    public static class CardPreview extends JPanel {
        private PaperCard card;
        private Image image;

        public CardPreview() {
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(final java.awt.event.MouseEvent e) {
                    zoom(card);
                }
            });
        }

        public void setCard(final PaperCard card0) {
            card = card0;
            image = null;
            repaint();
        }

        public void setImage(final Image image0) {
            card = null;
            image = image0;
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final int w = getWidth();
            final int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            final Graphics2D g2 = (Graphics2D) g;
            if (card != null) {
                int ch = h;
                int cw = Math.round(ch / 1.4f);
                if (cw > w) {
                    cw = w;
                    ch = Math.round(cw * 1.4f);
                }
                final BufferedImage img = ImageCache.getImage(card, cw, ch);
                if (img != null) {
                    g2.drawImage(img, (w - cw) / 2, (h - ch) / 2, cw, ch, null);
                }
            } else if (image != null) {
                drawContain(g2, image, 0, 0, w, h);
            }
        }
    }

    /** A list entry shown by its label (FList renders items with toString). */
    public record Named<T>(T value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public static final Color TEXT_COLOR = new Color(200, 207, 208);
    public static final Color BAR_COLOR = new Color(255, 255, 255, 38);
}
