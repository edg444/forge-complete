package forge.screens.home.conquest;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.google.common.collect.Iterables;

import forge.ImageCache;
import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.GuiChoose;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.IHasSkinProp;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.Aggregates;
import forge.util.IterableUtil;
import forge.util.MyRandom;
import net.miginfocom.swing.MigLayout;

/** Spend aether shards to pull a random card of the chosen color, type, rarity and mana value from the current plane. */
public final class VSubmenuConquestAether extends AConquestSubmenu {
    public static final VSubmenuConquestAether INSTANCE = new VSubmenuConquestAether();

    private static final int BUTTON_SIZE = 72;
    private static final int TICK_MS = 15;

    private final Set<PaperCard> pool = new HashSet<>();
    private final Set<PaperCard> filteredPool = new HashSet<>();
    private final Set<PaperCard> strictPool = new HashSet<>();
    private final AetherDisplay display = new AetherDisplay();
    private final FilterButton<ColorSet> btnColor;
    private final FilterButton<ConquestUtil.TypeFilter> btnType;
    private final FilterButton<ConquestUtil.RarityFilter> btnRarity;
    private final FilterButton<ConquestUtil.CMCFilter> btnCmc;
    private final FLabel lblShards = new FLabel.Builder().fontSize(16).build();
    private ConquestCommander commander;
    private int shardCost;
    private PaperCard pulledCard;
    private float pullProgress = -1;
    private Timer pullTimer;

    private VSubmenuConquestAether() {
        super("lblTheAether", true);
        // pink and gold are only ever a card's color, never something a deck is built around
        final ColorSet[] colorOptions = Arrays.stream(ColorSet.values()).filter(c -> c.hasNoColorsExcept(MagicColor.ALL_COLORS))
                .sorted(Comparator.comparing(ColorSet::getOrderWeight)).toArray(ColorSet[]::new);
        btnColor = new FilterButton<>("lblColor", colorOptions,
                c -> "Playable in " + c.stream().map(MagicColor.Color::getSymbol).collect(Collectors.joining()),
                (c, card) -> card.getRules().getColorIdentity().hasNoColorsExcept(c));
        btnType = new FilterButton<>("lblType", ConquestUtil.TypeFilter.values(), null, (f, card) -> f.test(card));
        btnRarity = new FilterButton<>("lblRarity", ConquestUtil.RarityFilter.values(), null, (f, card) -> f.test(card));
        btnCmc = new FilterButton<>("lblCMC", ConquestUtil.CMCFilter.values(), null, (f, card) -> f.test(card));
        lblShards.setIcon(ConquestSkin.getIcon(FSkinProp.IMG_AETHER_SHARD, 28));
        lblShards.setForeground(ConquestDesktop.TEXT_COLOR);

        display.setLayout(new MigLayout("insets 10, gap 10, fill"));
        display.add(btnColor, "w " + BUTTON_SIZE + "px!, h " + BUTTON_SIZE + "px!, ax left, ay top");
        display.add(btnType, "w " + BUTTON_SIZE + "px!, h " + BUTTON_SIZE + "px!, ax right, ay top, wrap");
        display.add(new JPanel() { { setOpaque(false); } }, "span 2, grow, push, wrap");
        display.add(btnRarity, "w " + BUTTON_SIZE + "px!, h " + BUTTON_SIZE + "px!, ax left, ay bottom, split 3");
        display.add(lblShards, "growx, pushx, ay center, gapleft 20");
        display.add(btnCmc, "w " + BUTTON_SIZE + "px!, h " + BUTTON_SIZE + "px!, ax right, ay bottom");
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTAETHER;
    }

    @Override
    protected void populateContent(final JPanel container) {
        container.add(display, CONTENT_CONSTRAINTS);
        final ConquestData model = ConquestDesktop.model();
        final ConquestPlane plane = model.getCurrentPlane();
        final ConquestCommander commander0 = model.getSelectedCommander();
        if (commander != commander0) {
            commander = commander0;
            resetFilters();
        }
        pool.clear();
        for (final PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card) && !card.getRules().getType().isBasicLand()) {
                pool.add(card);
            }
        }
        pulledCard = null;
        updateFilteredPool();
        updateAvailableShards();
    }

    private void resetFilters() {
        btnColor.setSelectedOption(commander.getCard().getRules().getColorIdentity());
        btnType.setSelectedOption(ConquestUtil.TypeFilter.CREATURE);
        btnRarity.setSelectedOption(ConquestUtil.RarityFilter.COMMON);
        btnCmc.setSelectedOption(ConquestUtil.CMCFilter.CMC_LOW_MID);
    }

    private void updateFilteredPool() {
        final Predicate<PaperCard> predicate = btnColor.and(btnType).and(btnRarity).and(btnCmc);
        final CardRarity selectedRarity = btnRarity.selectedOption.getRarity();
        filteredPool.clear();
        strictPool.clear();
        for (final PaperCard card : pool) {
            if (predicate.test(card)) {
                filteredPool.add(card);
                if (selectedRarity == card.getRarity()) {
                    strictPool.add(card);
                }
            } else if (card.getRarity() == CardRarity.BasicLand
                    && !card.isVeryBasicLand()
                    && !card.getName().equals("Wastes")
                    && !MagicColor.Constant.SNOW_LANDS.contains(card.getName())
                    && selectedRarity == CardRarity.Common
                    && btnCmc.selectedOption == ConquestUtil.CMCFilter.CMC_LOW
                    && card.getRules().getColorIdentity().hasNoColorsExcept(commander.getCard().getRules().getColorIdentity())) {
                filteredPool.add(card);
            }
        }
        shardCost = filteredPool.isEmpty() ? 0 : ConquestUtil.getShardValue(btnRarity.selectedOption.getRarity(), CQPref.AETHER_BASE_PULL_COST);
        display.repaint();
    }

    private void updateAvailableShards() {
        lblShards.setText(localizer.getMessage("lblAetherShards") + ": " + ConquestDesktop.model().getAEtherShards());
        display.repaint();
    }

    private void pullFromTheAether() {
        if (filteredPool.isEmpty() || strictPool.isEmpty()) {
            return;
        }
        final ConquestData model = ConquestDesktop.model();
        if (model.getAEtherShards() < shardCost) {
            return;
        }
        Iterable<PaperCard> rewardPool;
        final CardRarity minRarity = btnRarity.selectedOption.getRarity();
        CardRarity rarity = btnRarity.selectedOption.getRarity(MyRandom.getRandom().nextDouble());
        while (true) {
            final CardRarity allowedRarity = rarity;
            rewardPool = IterableUtil.filter(filteredPool, card -> allowedRarity == card.getRarity()
                    || allowedRarity == CardRarity.Rare && card.getRarity() == CardRarity.Special
                    || allowedRarity == CardRarity.Common && card.getRarity() == CardRarity.BasicLand);
            if (Iterables.isEmpty(rewardPool)) {
                if (rarity == minRarity) {
                    return;
                }
                switch (rarity) {
                    case MythicRare -> rarity = CardRarity.Rare;
                    case Rare -> rarity = CardRarity.Uncommon;
                    case Uncommon -> rarity = CardRarity.Common;
                    default -> {
                        return;
                    }
                }
                continue;
            }
            break;
        }
        final PaperCard card = Aggregates.random(rewardPool);
        if (card == null) {
            return;
        }
        pool.remove(card);
        filteredPool.remove(card);
        strictPool.remove(card);
        model.spendAEtherShards(shardCost);
        model.unlockCard(card);
        model.saveData();
        updateFilteredPool();
        updateAvailableShards();

        pulledCard = card;
        pullProgress = 0;
        if (pullTimer != null) {
            pullTimer.stop();
        }
        pullTimer = new Timer(TICK_MS, e -> {
            pullProgress += TICK_MS / 1000f;
            if (pullProgress >= 0.6f) {
                pullProgress = -1;
                ((Timer) e.getSource()).stop();
            }
            display.repaint();
        });
        pullTimer.start();
    }

    private String buildMessage() {
        final String cost = shardCost == 0 ? "--" : String.valueOf(shardCost);
        return localizer.getMessage("lblClickToPullFromAE", cost);
    }

    @SuppressWarnings("serial")
    private final class AetherDisplay extends JPanel {
        private AetherDisplay() {
            setOpaque(true);
            setBackground(Color.BLACK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || pullProgress >= 0) {
                        return;
                    }
                    if (pulledCard != null) {
                        if (getCardBounds(1).contains(e.getPoint())) {
                            ConquestDesktop.zoom(pulledCard);
                        } else {
                            pulledCard = null;
                            repaint();
                        }
                        return;
                    }
                    pullFromTheAether();
                }
            });
        }

        private Rectangle getCardBounds(final float t) {
            final int w = getWidth();
            final int h = getHeight();
            final float endHeight = h - 2 * BUTTON_SIZE - 40;
            final float startHeight = h * 0.05f;
            final float ch = startHeight + (endHeight - startHeight) * t;
            final float cw = ch / 1.4f;
            return new Rectangle(Math.round((w - cw) / 2), Math.round((h - ch) / 2), Math.round(cw), Math.round(ch));
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            super.paintComponent(g0);
            final Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final int w = getWidth();
            final int h = getHeight();
            final BufferedImage space = ConquestSkin.getSpace();
            if (space != null) {
                final int bgHeight = w * space.getHeight() / space.getWidth();
                g.drawImage(space, 0, h - bgHeight, w, bgHeight, null);
                // mirrored copy on top, clipped to the upper half, as on mobile
                final Graphics2D top = (Graphics2D) g.create();
                top.clipRect(0, 0, w, h / 2);
                top.drawImage(space, 0, bgHeight, w, -bgHeight, null);
                top.dispose();
            }
            if (pulledCard != null) {
                final float t = pullProgress < 0 ? 1 : Math.min(1, pullProgress / 0.6f);
                final Rectangle b = getCardBounds(t);
                final BufferedImage img = ImageCache.getImage(pulledCard, b.width, b.height);
                if (img != null) {
                    g.drawImage(img, b.x, b.y, b.width, b.height, null);
                }
                return;
            }
            final ConquestData model = ConquestDesktop.model();
            if (model == null) {
                return;
            }
            final Font font = FSkin.getRelativeFont(16).getBaseFont();
            g.setFont(font);
            final FontMetrics fm = g.getFontMetrics();
            final String line1 = buildMessage();
            final String line2 = localizer.getMessage("lblPullPoolSizes", strictPool.size(), filteredPool.size(), pool.size());
            final boolean cantAfford = shardCost > 0 && model.getAEtherShards() < shardCost;
            final boolean empty = strictPool.isEmpty();
            int y = h / 2 - fm.getHeight() / 2;
            g.setColor(cantAfford ? Color.RED : Color.WHITE);
            g.drawString(line1, (w - fm.stringWidth(line1)) / 2, y);
            y += fm.getHeight() + 6;
            g.setColor(empty ? Color.RED : Color.WHITE);
            g.drawString(line2, (w - fm.stringWidth(line2)) / 2, y);
        }
    }

    /** One filter's button: shows the selected option's icon and opens a chooser when clicked. */
    @SuppressWarnings("serial")
    private final class FilterButton<T> extends JPanel implements Predicate<PaperCard> {
        private final String caption;
        private final List<T> options;
        private final forge.util.FSerializableFunction<T, String> displayFn;
        private final java.util.function.BiPredicate<T, PaperCard> test;
        private T selectedOption;
        private boolean hovered;

        private FilterButton(final String captionKey, final T[] options0, final forge.util.FSerializableFunction<T, String> display0,
                final java.util.function.BiPredicate<T, PaperCard> test0) {
            caption = localizer.getMessage(captionKey);
            options = List.of(options0);
            displayFn = display0;
            test = test0;
            selectedOption = options.get(0);
            setOpaque(false);
            setToolTipText(localizer.getMessage("lblSelectCaptionFilter", caption));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    choose();
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void choose() {
            final List<T> result = GuiChoose.getChoices(localizer.getMessage("lblSelectCaptionFilter", caption), 0, 1,
                    options, List.of(selectedOption), displayFn);
            if (result != null && !result.isEmpty()) {
                setSelectedOption(result.get(0));
                updateFilteredPool();
            }
        }

        private void setSelectedOption(final T option) {
            selectedOption = option;
            repaint();
        }

        @Override
        public boolean test(final PaperCard card) {
            return test.test(selectedOption, card);
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            super.paintComponent(g0);
            final Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            final int w = getWidth();
            final int h = getHeight();
            g.setColor(hovered ? new Color(255, 255, 255, 64) : ConquestDesktop.BAR_COLOR);
            g.fillRect(0, 0, w, h);
            final int pad = 6;
            if (selectedOption instanceof ColorSet colors) {
                final int count = Math.max(1, colors.getOrderedColors().size());
                final int size = Math.min(h - 2 * pad, (w - 2 * pad) / count);
                CardFaceSymbols.drawColorSet(g, colors, (w - size * count) / 2, (h - size) / 2, size, false);
            } else if (selectedOption instanceof IHasSkinProp prop) {
                final BufferedImage icon = ConquestSkin.getImage(prop.getSkinProp());
                if (icon != null) {
                    ConquestDesktop.drawContain(g, icon, pad, pad, w - 2 * pad, h - 2 * pad);
                }
            }
        }
    }
}
