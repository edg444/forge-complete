package forge.screens.home.conquest;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import forge.ImageCache;
import forge.ImageKeys;
import forge.gamemodes.planarconquest.ConquestReward;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FButton;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FDialog;

/**
 * Reveals reward cards one at a time with a flip, like the mobile reward dialog. Duplicates fade
 * and show the aether shards given in their place. Modal: returns once the player clicks OK.
 */
@SuppressWarnings("serial")
public final class ConquestRewardDialog extends FDialog {
    private static final float DURATION_PER_CARD = 0.7f;
    private static final float FLIP_DURATION = 0.85f;
    private static final float FADE_DUPLICATE_DURATION = 0.1f;
    private static final float DUPLICATE_ALPHA = 0.35f;
    private static final int TICK_MS = 15;
    private static final int GAP = 10;

    private final List<ConquestReward> rewards;
    private final float[] progress;
    private final RevealPanel revealPanel = new RevealPanel();
    private final FButton btnOk = new FButton(Localizer.getInstance().getMessage("lblOK"));
    private final FButton btnSkip = new FButton(Localizer.getInstance().getMessage("lblSkip"));
    private final Timer timer;
    private float elapsed = -0.25f;
    private int currentIndex;

    public static void show(final String title, final Iterable<ConquestReward> rewards) {
        final List<ConquestReward> list = new ArrayList<>();
        rewards.forEach(list::add);
        if (list.isEmpty()) {
            return;
        }
        final ConquestRewardDialog dialog = new ConquestRewardDialog(title, list);
        dialog.setVisible(true);
        dialog.dispose();
    }

    private ConquestRewardDialog(final String title, final List<ConquestReward> rewards0) {
        super(true, true, "10");
        rewards = rewards0;
        progress = new float[rewards.size()];
        setTitle(title);

        final int columns = Math.min(rewards.size(), 5);
        final int rowsShown = Math.min(2, (rewards.size() + columns - 1) / columns);
        final int cardWidth = 180;
        final int cardHeight = Math.round(cardWidth * 1.4f);
        revealPanel.columns = columns;
        revealPanel.cardWidth = cardWidth;
        revealPanel.cardHeight = cardHeight;

        final FScrollPane scroller = new FScrollPane(revealPanel, false);
        scroller.getVerticalScrollBar().setUnitIncrement(24);
        final int viewWidth = columns * cardWidth + (columns + 1) * GAP + 20;
        final int viewHeight = rowsShown * cardHeight + (rowsShown + 1) * GAP;
        add(scroller, "w " + viewWidth + "px!, h " + viewHeight + "px!, span 2, wrap, gapbottom 10");
        add(btnOk, "w 150px!, h 32px!, ax right, gapright 10");
        add(btnSkip, "w 150px!, h 32px!, ax left");
        pack();

        btnOk.setEnabled(false);
        btnOk.setCommand(() -> setVisible(false));
        btnSkip.setCommand(this::skip);
        timer = new Timer(TICK_MS, e -> advance(TICK_MS / 1000f));
        timer.start();
    }

    @Override
    public void setVisible(final boolean visible) {
        if (!visible && timer != null) {
            timer.stop();
        }
        super.setVisible(visible);
    }

    private void advance(final float dt) {
        elapsed += dt;
        if (elapsed <= 0) {
            return;
        }
        final int count = rewards.size();
        int index = Math.min(count, (int) (elapsed / DURATION_PER_CARD));
        if (index > currentIndex) {
            for (int i = currentIndex; i < index; i++) {
                progress[i] = 1;
            }
            if (index < count) {
                revealPanel.scrollRectToVisible(revealPanel.getCardBounds(index));
            }
        }
        currentIndex = index;
        if (currentIndex == count) {
            finish();
        } else {
            progress[currentIndex] = (elapsed - currentIndex * DURATION_PER_CARD) / DURATION_PER_CARD;
        }
        revealPanel.repaint();
    }

    private void skip() {
        for (int i = currentIndex; i < rewards.size(); i++) {
            progress[i] = 1;
        }
        currentIndex = rewards.size();
        finish();
        revealPanel.repaint();
    }

    private void finish() {
        timer.stop();
        btnOk.setEnabled(true);
        btnSkip.setEnabled(false);
        btnOk.requestFocusInWindow();
    }

    private final class RevealPanel extends JPanel {
        private int columns;
        private int cardWidth;
        private int cardHeight;

        private RevealPanel() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    for (int i = 0; i < currentIndex; i++) {
                        if (getCardBounds(i).contains(e.getPoint())) {
                            ConquestDesktop.zoom(rewards.get(i).getCard());
                            return;
                        }
                    }
                }
            });
        }

        private Rectangle getCardBounds(final int index) {
            final int row = index / columns;
            final int col = index % columns;
            return new Rectangle(GAP + col * (cardWidth + GAP), GAP + row * (cardHeight + GAP), cardWidth, cardHeight);
        }

        @Override
        public Dimension getPreferredSize() {
            final int rows = (rewards.size() + columns - 1) / columns;
            return new Dimension(columns * cardWidth + (columns + 1) * GAP, rows * cardHeight + (rows + 1) * GAP);
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            super.paintComponent(g0);
            final Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = 0; i < rewards.size(); i++) {
                drawReward(g, rewards.get(i), progress[i], getCardBounds(i));
            }
        }

        private void drawReward(final Graphics2D g, final ConquestReward reward, final float p, final Rectangle b) {
            if (p >= FLIP_DURATION) {
                final float fade = (p - FLIP_DURATION) / FADE_DUPLICATE_DURATION;
                final Graphics2D g2 = (Graphics2D) g.create();
                if (reward.isDuplicate()) {
                    float alpha = DUPLICATE_ALPHA;
                    if (fade < 1) {
                        alpha += (1 - fade) * (1 - DUPLICATE_ALPHA);
                    }
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                }
                drawCard(g2, ImageCache.getImage(reward.getCard(), b.width, b.height), b.x, b.width, b);
                g2.dispose();
                if (reward.isDuplicate() && fade >= 1) {
                    drawShards(g, reward.getReplacementShards(), b);
                }
            } else {
                final float half = FLIP_DURATION / 2;
                if (p >= half) {
                    final int flipWidth = Math.round(b.width * (p - half) / half);
                    drawCard(g, ImageCache.getImage(reward.getCard(), b.width, b.height), b.x + (b.width - flipWidth) / 2, flipWidth, b);
                } else {
                    final BufferedImage back = ImageCache.scaleImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), b.width, b.height, true, null);
                    final int flipWidth = Math.round(b.width * (half - p) / half);
                    drawCard(g, back, b.x + (b.width - flipWidth) / 2, flipWidth, b);
                }
            }
        }

        private void drawCard(final Graphics2D g, final BufferedImage img, final int x, final int w, final Rectangle b) {
            if (img != null && w > 0) {
                g.drawImage(img, x, b.y, w, b.height, null);
            }
        }

        private void drawShards(final Graphics2D g, final int shards, final Rectangle b) {
            final BufferedImage icon = ConquestSkin.getImage(FSkinProp.IMG_AETHER_SHARD);
            final int size = b.width / 3;
            final Font font = FSkin.getRelativeBoldFont(22).getBaseFont();
            g.setFont(font);
            final FontMetrics fm = g.getFontMetrics();
            final String text = String.valueOf(shards);
            final int total = size + 4 + fm.stringWidth(text);
            final int x = b.x + (b.width - total) / 2;
            final int y = b.y + (b.height - size) / 2;
            if (icon != null) {
                g.drawImage(icon, x, y, size, size, null);
            }
            g.setColor(Color.BLACK);
            g.drawString(text, x + size + 5, y + (size + fm.getAscent()) / 2 + 1);
            g.setColor(Color.WHITE);
            g.drawString(text, x + size + 4, y + (size + fm.getAscent()) / 2);
        }
    }
}
