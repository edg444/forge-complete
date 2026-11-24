package forge.screens.home.conquest;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.gamemodes.planarconquest.ConquestAwardPool;
import forge.gamemodes.planarconquest.ConquestBattle;
import forge.gamemodes.planarconquest.ConquestChaosBattle;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestEvent;
import forge.gamemodes.planarconquest.ConquestEvent.ConquestEventRecord;
import forge.gamemodes.planarconquest.ConquestLocation;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPlaneData;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestRegion;
import forge.gamemodes.planarconquest.ConquestReward;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.gui.framework.EDocID;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.collect.FCollectionView;
import net.miginfocom.swing.MigLayout;

/** The plane map: travel between events, battle the current one, and collect the rewards afterwards. */
public final class VSubmenuConquestMultiverse extends AConquestSubmenu {
    public static final VSubmenuConquestMultiverse INSTANCE = new VSubmenuConquestMultiverse();

    private static final Color FOG_OF_WAR_COLOR = new Color(0, 0, 0, 166);
    private static final Color UNCONQUERED_COLOR = new Color(0, 0, 0, 26);
    private static final int TICK_MS = 15;

    private final PlaneGrid planeGrid = new PlaneGrid();
    private final FScrollPane scroller = new FScrollPane(planeGrid, false);
    private final SidePanel sidePanel = new SidePanel();
    private final JPanel content = new JPanel(new MigLayout("insets 0, gap 10, fill"));

    private ConquestData model;
    private ConquestBattle activeBattle;
    private ConquestReward regionCommander;

    private VSubmenuConquestMultiverse() {
        super("lblTheMultiverse", true);
        content.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(24);
        // the grid's height follows its width, so a new width needs a new preferred size
        scroller.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(final java.awt.event.ComponentEvent e) {
                planeGrid.revalidate();
                SwingUtilities.invokeLater(planeGrid::scrollPlaneswalkerIntoView);
            }
        });
        content.add(scroller, "grow, push");
        content.add(sidePanel, "w 300px:32%:420px, growy, pushy");
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTMULTIVERSE;
    }

    @Override
    protected void populateContent(final JPanel display) {
        display.add(content, CONTENT_CONSTRAINTS);
        model = ConquestDesktop.model();
        sidePanel.update();
        planeGrid.revalidate();
        planeGrid.repaint();
        SwingUtilities.invokeLater(() -> {
            planeGrid.scrollPlaneswalkerIntoView();
            if (activeBattle != null && activeBattle.isFinished()) {
                onBattleFinished();
            }
        });
    }

    private boolean isAnimating() {
        return planeGrid.moveAnimation != null || planeGrid.badgeAnimation != null;
    }

    // Rewards are handed out when the map is shown again after the win/lose screen, as on mobile.
    private void onBattleFinished() {
        final ConquestBattle battle = activeBattle;
        activeBattle = null;
        if (!battle.wasConquered()) {
            return;
        }
        if (battle instanceof ConquestChaosBattle chaosBattle) {
            awardBoosters(chaosBattle.getAwardPool(), 3);
            return;
        }
        final ConquestLocation loc = battle.getLocation();
        final ConquestEventRecord record = model.getCurrentPlaneData().getEventRecord(loc);
        if (record.getWins(battle.getTier()) == 1 && record.getHighestConqueredTier() == battle.getTier()) {
            model.rewardPlaneswalkEmblems(FModel.getConquestPreferences().getPrefInt(CQPref.PLANESWALK_CONQUER_EMBLEMS));
            regionCommander = FModel.getConquest().awardRegionCommander(loc);
            model.saveData();
            planeGrid.animateBadgeIntoPosition(loc, battle.getTier());
        } else {
            spinChaosWheel();
        }
    }

    private void showRegionCommander() {
        if (regionCommander != null) {
            final ConquestReward reward = regionCommander;
            regionCommander = null;
            final String title = localizer.getMessage("lblConqueredNReceivedCommander",
                    model.getCurrentLocation().getRegion().getName()).replace("\n", " - ");
            ConquestRewardDialog.show(title, List.of(reward));
        }
        spinChaosWheel();
    }

    private void spinChaosWheel() {
        final ConquestEvent.ChaosWheelOutcome outcome = ConquestChaosWheel.spin();
        final int wheelShards = FModel.getConquestPreferences().getPrefInt(CQPref.AETHER_WHEEL_SHARDS);
        switch (outcome) {
            case BOOSTER -> awardBoosters(model.getCurrentPlane().getAwardPool(), 1);
            case DOUBLE_BOOSTER -> awardBoosters(model.getCurrentPlane().getAwardPool(), 2);
            case SHARDS -> awardShards(wheelShards, false);
            case DOUBLE_SHARDS -> awardShards(2 * wheelShards, false);
            case PLANESWALK -> {
                if (model.getUnlockedPlaneCount() == model.getAccessiblePlaneCount()) {
                    FOptionPane.showMessageDialog(localizer.getMessage("lblAllPlanesUnlockedNotify"),
                            localizer.getMessage("lblAllPlanesUnlocked"), ConquestSkin.getIcon(FSkinProp.IMG_PW_BADGE_COMMON, 96));
                    launchChaosBattle();
                } else {
                    awardBonusPlaneswalkEmblems(FModel.getConquestPreferences().getPrefInt(CQPref.PLANESWALK_WHEEL_EMBLEMS));
                }
            }
            case CHAOS -> launchChaosBattle();
        }
    }

    private void awardBoosters(final ConquestAwardPool pool, final int totalCount) {
        final int shardsBefore = model.getAEtherShards();
        for (int number = 1; number <= totalCount; number++) {
            final String title = totalCount > 1
                    ? localizer.getMessage("lblReceivedBoosterPackNOfTotal", String.valueOf(number), String.valueOf(totalCount)).replace("\n", " ")
                    : localizer.getMessage("lblReceivedBoosterPack");
            ConquestRewardDialog.show(title, FModel.getConquest().awardBooster(pool));
        }
        final int shardsReceived = model.getAEtherShards() - shardsBefore;
        if (shardsReceived > 0) {
            awardShards(shardsReceived, true);
        }
        model.saveData();
        sidePanel.update();
    }

    private void awardShards(final int shards, final boolean fromDuplicateCards) {
        final String message;
        if (fromDuplicateCards) {
            message = localizer.getMessage("lblReceivedAetherShardsForDuplicateCards");
        } else {
            message = localizer.getMessage("lblReceivedAetherShards");
            model.rewardAEtherShards(shards);
            model.saveData();
        }
        FOptionPane.showMessageDialog(String.valueOf(shards), message, ConquestSkin.getIcon(FSkinProp.IMG_AETHER_SHARD, 96));
        sidePanel.update();
    }

    private void awardBonusPlaneswalkEmblems(final int emblems) {
        model.rewardPlaneswalkEmblems(emblems);
        model.saveData();
        FOptionPane.showMessageDialog(String.valueOf(emblems), localizer.getMessage("lblReceivedBonusPlaneswalkEmblems"),
                ConquestSkin.getIcon(FSkinProp.IMG_PW_BADGE_COMMON, 96));
        sidePanel.update();
    }

    private void launchEvent() {
        if (isAnimating()) {
            return;
        }
        final ConquestLocation loc = model.getCurrentLocation();
        startBattle(loc.getEvent().createBattle(loc, 0));
    }

    private void launchChaosBattle() {
        startBattle(new ConquestChaosBattle());
    }

    private void startBattle(final ConquestBattle battle) {
        // A battle left without its win/lose screen (closing the match tab) never got finished, and the
        // controller refuses to start another until it is.
        if (activeBattle != null && !activeBattle.isFinished()) {
            FModel.getConquest().finishEvent(new StubWinLoseView());
        }
        activeBattle = battle;
        FModel.getConquest().startBattle(battle);
    }

    /** Lets an abandoned battle be finished without a win/lose screen; nothing here is shown. */
    private static final class StubWinLoseView implements forge.gui.interfaces.IWinLoseView<FButton> {
        private final FButton btnContinue = new FButton();
        private final FButton btnRestart = new FButton();
        private final FButton btnQuit = new FButton();

        private StubWinLoseView() {
            btnContinue.setVisible(false);
        }

        @Override public FButton getBtnContinue() { return btnContinue; }
        @Override public FButton getBtnRestart() { return btnRestart; }
        @Override public FButton getBtnQuit() { return btnQuit; }
        @Override public void hide() { }
        @Override public void showRewards(final Runnable runnable) { runnable.run(); }
        @Override public void showCards(final String title, final List<forge.item.PaperCard> cards) { }
        @Override public void showMessage(final String message, final String title, final FSkinProp icon) { }
    }

    @SuppressWarnings("serial")
    private final class PlaneGrid extends JPanel implements Scrollable {
        private MoveAnimation moveAnimation;
        private BadgeAnimation badgeAnimation;

        private PlaneGrid() {
            setOpaque(true);
            setBackground(Color.BLACK);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (model == null || isAnimating() || !SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    final ConquestLocation loc = getLocation(e.getX(), e.getY());
                    if (loc != null && !model.getCurrentLocation().equals(loc)) {
                        final List<ConquestLocation> path = model.getPath(loc);
                        if (path != null) {
                            moveAnimation = new MoveAnimation(path);
                            moveAnimation.start();
                        }
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(final MouseEvent e) {
                    if (model == null) {
                        return;
                    }
                    final ConquestLocation loc = getLocation(e.getX(), e.getY());
                    final boolean reachable = loc != null && !model.getCurrentLocation().equals(loc) && model.getPath(loc) != null;
                    setCursor(Cursor.getPredefinedCursor(reachable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });
        }

        private ConquestPlane plane() {
            return model.getCurrentPlane();
        }

        private float regionHeight() {
            return getWidth() / ConquestCardArt.CARD_ART_RATIO;
        }

        @Override
        public Dimension getPreferredSize() {
            final int width = getParent() instanceof JViewport vp ? vp.getWidth() : Math.max(getWidth(), 600);
            if (model == null) {
                return new Dimension(width, 400);
            }
            return new Dimension(width, Math.round(plane().getRegions().size() * width / ConquestCardArt.CARD_ART_RATIO));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return visibleRect.height - 24;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private ConquestLocation getLocation(final int x, final int y) {
            final ConquestPlane plane = plane();
            final int rowsPerRegion = plane.getRowsPerRegion();
            final int cols = plane.getCols();
            final float h = getHeight();
            final float rowHeight = regionHeight() / rowsPerRegion;
            final float colWidth = (float) getWidth() / cols;
            final int rowIndex = (int) ((h - y) / rowHeight);
            final int regionIndex = rowIndex / rowsPerRegion;
            if (regionIndex < 0 || regionIndex >= plane.getRegions().size()) {
                return null;
            }
            final int row = rowIndex % rowsPerRegion;
            final int col = Math.max(0, Math.min(cols - 1, (int) (x / colWidth)));
            return new ConquestLocation(plane, regionIndex, row, col);
        }

        private Point2D.Float getPosition(final ConquestLocation loc) {
            final float h = getHeight();
            final float regionHeight = regionHeight();
            final float colWidth = (float) getWidth() / loc.getPlane().getCols();
            final float rowHeight = regionHeight / loc.getPlane().getRowsPerRegion();
            final float x = loc.getCol() * colWidth + colWidth / 2;
            final float y = h - (loc.getRegionIndex() * regionHeight + loc.getRow() * rowHeight + rowHeight / 2);
            return new Point2D.Float(x, y);
        }

        private void scrollPlaneswalkerIntoView() {
            if (model == null || getWidth() == 0) {
                return;
            }
            final float rowHeight = regionHeight() / plane().getRowsPerRegion();
            final Point2D.Float pos = moveAnimation == null ? getPosition(model.getCurrentLocation()) : moveAnimation.pos;
            final int margin = Math.round(rowHeight * 1.5f);
            scrollRectToVisible(new Rectangle(Math.round(pos.x) - 1, Math.round(pos.y) - margin, 2, 2 * margin));
        }

        private void animateBadgeIntoPosition(final ConquestLocation loc, final int tier) {
            badgeAnimation = new BadgeAnimation(loc, tier);
            badgeAnimation.start();
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            super.paintComponent(g0);
            if (model == null) {
                return;
            }
            final Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            final ConquestPlane plane = plane();
            final int w = getWidth();
            final float regionHeight = regionHeight();
            final int cols = plane.getCols();
            final int rows = plane.getRowsPerRegion();
            final float colWidth = (float) w / cols;
            final float rowHeight = regionHeight / rows;
            final float iconSize = Math.min(colWidth, rowHeight) / 3;
            final float iconOffset = Math.round(iconSize * 0.1f);
            final FCollectionView<ConquestRegion> regions = plane.getRegions();
            final int regionCount = regions.size();
            final ConquestPlaneData planeData = model.getCurrentPlaneData();
            final Rectangle clip = g.getClipBounds();

            float y = 0;
            for (int i = regionCount - 1; i >= 0; i--) {
                if (clip != null && (y + regionHeight < clip.y || y > clip.y + clip.height)) {
                    y += regionHeight;
                    continue;
                }
                final ConquestRegion region = regions.get(i);
                final Image art = ConquestDesktop.toImage(region.getArt());
                if (art != null) {
                    g.drawImage(art, 0, Math.round(y), w, Math.round(y + regionHeight) - Math.round(y), null);
                } else {
                    fillColorBackground(g, region.getColorSet(), 0, Math.round(y), w, Math.round(regionHeight));
                }
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        float x0 = c * colWidth;
                        float y0 = y + (rows - r - 1) * rowHeight;
                        final ConquestEventRecord eventRecord = planeData.getEventRecord(i, r, c);
                        if (eventRecord != null && eventRecord.hasConquered()) {
                            x0 = Math.round(x0 + colWidth - iconOffset - iconSize * 0.9f);
                            y0 = Math.round(y0 + iconOffset);
                            int tier = eventRecord.getHighestConqueredTier();
                            if (badgeAnimation != null && badgeAnimation.location.isAt(i, r, c)) {
                                badgeAnimation.end.setRect(x0, y0, iconSize, iconSize);
                                tier--; // the new badge is still flying in
                            }
                            if (tier >= 0) {
                                final BufferedImage badge = ConquestSkin.getBadge(tier);
                                if (badge != null) {
                                    g.drawImage(badge, Math.round(x0), Math.round(y0), Math.round(iconSize), Math.round(iconSize), null);
                                }
                            }
                        } else {
                            Color color = FOG_OF_WAR_COLOR;
                            if (i == 0 && r == 0 && c == 0) {
                                color = UNCONQUERED_COLOR;
                            } else {
                                for (final ConquestLocation loc : ConquestLocation.getNeighbors(plane, i, r, c)) {
                                    if (planeData.hasConquered(loc)) {
                                        color = UNCONQUERED_COLOR;
                                        break;
                                    }
                                }
                            }
                            g.setColor(color);
                            g.fillRect(Math.round(x0), Math.round(y0), Math.round(x0 + colWidth) - Math.round(x0),
                                    Math.round(y0 + rowHeight) - Math.round(y0));
                        }
                    }
                }
                g.setColor(Color.BLACK);
                float y0 = y;
                for (int r = 0; r < rows; r++) {
                    g.drawLine(0, Math.round(y0), w, Math.round(y0));
                    y0 += rowHeight;
                }
                y += regionHeight;
            }
            g.setColor(Color.BLACK);
            float x0 = colWidth;
            for (int c = 1; c < cols; c++) {
                g.drawLine(Math.round(x0), 0, Math.round(x0), getHeight());
                x0 += colWidth;
            }

            final Image token = ConquestDesktop.toImage(model.getPlaneswalkerToken());
            if (token != null) {
                final float tokenHeight = rowHeight * 0.85f;
                final float tokenWidth = tokenHeight * token.getWidth(null) / token.getHeight(null);
                final Point2D.Float pos = moveAnimation == null ? getPosition(model.getCurrentLocation()) : moveAnimation.pos;
                g.drawImage(token, Math.round(pos.x - tokenWidth / 2), Math.round(pos.y - tokenHeight / 2),
                        Math.round(tokenWidth), Math.round(tokenHeight), null);
            }
            if (badgeAnimation != null) {
                badgeAnimation.drawBadge(g);
            }
        }

        private static void fillColorBackground(final Graphics2D g, final ColorSet colors, final int x, final int y, final int w, final int h) {
            final List<DetailColors> backColors = CardDetailUtil.getBorderColors(colors);
            if (backColors.isEmpty()) {
                return;
            }
            final DetailColors first = backColors.get(0);
            final DetailColors last = backColors.get(backColors.size() - 1);
            g.setPaint(new GradientPaint(x, y, new Color(first.r, first.g, first.b), x + w, y, new Color(last.r, last.g, last.b)));
            g.fillRect(x, y, w, h);
        }

        private final class MoveAnimation {
            private static final float DURATION_PER_SEGMENT = 0.5f;
            private final List<ConquestLocation> path;
            private final float duration;
            private final Timer timer;
            private Point2D.Float pos;
            private float progress;

            private MoveAnimation(final List<ConquestLocation> path0) {
                path = path0;
                pos = getPosition(path.get(0));
                duration = (path.size() - 1) * DURATION_PER_SEGMENT;
                timer = new Timer(TICK_MS, e -> advance(TICK_MS / 1000f));
            }

            private void start() {
                sidePanel.update();
                timer.start();
            }

            private void advance(final float dt) {
                progress += dt;
                if (progress >= duration) {
                    timer.stop();
                    pos = getPosition(path.get(path.size() - 1));
                    finish();
                    return;
                }
                final int segment = (int) (progress / DURATION_PER_SEGMENT);
                final float r = (progress - segment * DURATION_PER_SEGMENT) / DURATION_PER_SEGMENT;
                final Point2D.Float p1 = getPosition(path.get(segment));
                final Point2D.Float p2 = getPosition(path.get(segment + 1));
                pos = new Point2D.Float((1 - r) * p1.x + r * p2.x, (1 - r) * p1.y + r * p2.y);
                scrollPlaneswalkerIntoView();
                repaint();
            }

            private void finish() {
                final String secretArea = model.getCurrentLocation().getEvent().getTemporaryUnlock();
                if (secretArea != null) {
                    ConquestUtil.setPlaneTemporarilyAccessible(secretArea, false);
                }
                model.setCurrentLocation(path.get(path.size() - 1));
                model.saveData();
                moveAnimation = null;
                scrollPlaneswalkerIntoView();
                repaint();
                sidePanel.update();
            }
        }

        private final class BadgeAnimation {
            private static final float DURATION = 0.75f;
            private final ConquestLocation location;
            private final BufferedImage badge;
            private final Rectangle2D.Float start;
            private final Rectangle2D.Float end = new Rectangle2D.Float();
            private final Timer timer;
            private float progress = -0.5f;

            private BadgeAnimation(final ConquestLocation location0, final int tier) {
                location = location0;
                badge = ConquestSkin.getBadge(tier);
                final Rectangle view = getVisibleRect();
                final float startSize = Math.min(view.width, view.height) * 0.75f;
                start = new Rectangle2D.Float(view.x + (view.width - startSize) / 2, view.y + (view.height - startSize) / 2, startSize, startSize);
                timer = new Timer(TICK_MS, e -> advance(TICK_MS / 1000f));
            }

            private void start() {
                sidePanel.update();
                timer.start();
            }

            private void advance(final float dt) {
                progress += dt;
                repaint();
                if (progress >= DURATION + 0.5f) {
                    timer.stop();
                    badgeAnimation = null;
                    repaint();
                    sidePanel.update();
                    SwingUtilities.invokeLater(VSubmenuConquestMultiverse.this::showRegionCommander);
                }
            }

            private void drawBadge(final Graphics2D g) {
                if (badge == null) {
                    return;
                }
                final float t = Math.max(0, Math.min(1, progress / DURATION));
                final float x = start.x + (end.x - start.x) * t;
                final float y = start.y + (end.y - start.y) * t;
                final float w = start.width + (end.width - start.width) * t;
                final float h = start.height + (end.height - start.height) * t;
                g.drawImage(badge, Math.round(x), Math.round(y), Math.round(w), Math.round(h), null);
            }
        }
    }

    /** Current location, both commanders, and the Battle button. */
    @SuppressWarnings("serial")
    private final class SidePanel extends JPanel {
        private final FLabel lblRegion = new FLabel.Builder().fontSize(18).fontStyle(java.awt.Font.BOLD).fontAlign(SwingConstants.CENTER).build();
        private final ColorSetPanel pnlColors = new ColorSetPanel();
        private final FLabel lblEvent = new FLabel.Builder().fontSize(14).fontAlign(SwingConstants.CENTER).build();
        private final FLabel lblOpponent = new FLabel.Builder().fontSize(13).fontAlign(SwingConstants.CENTER).build();
        private final ConquestDesktop.CardPreview opponentPreview = new ConquestDesktop.CardPreview();
        private final FButton btnBattle = new FButton(localizer.getMessage("lblBattle"));
        private final ConquestDesktop.CardPreview commanderPreview = new ConquestDesktop.CardPreview() {
            @Override
            public void setCard(final forge.item.PaperCard card0) {
                super.setCard(card0);
                setToolTipText(localizer.getMessage("lblSelectCommander"));
            }
        };
        private final FLabel lblCommander = new FLabel.Builder().fontSize(13).fontAlign(SwingConstants.CENTER).build();
        private final FLabel lblShards = new FLabel.Builder().fontSize(13).build();
        private final FLabel lblEmblems = new FLabel.Builder().fontSize(13).build();
        private final FLabel lblHint = new FLabel.Builder().fontSize(11).fontAlign(SwingConstants.CENTER).build();

        private SidePanel() {
            super(new MigLayout("insets 10, gap 0, wrap, fill"));
            setOpaque(false);
            for (final FLabel lbl : new FLabel[] {lblRegion, lblEvent, lblOpponent, lblCommander, lblShards, lblEmblems, lblHint}) {
                lbl.setForeground(ConquestDesktop.TEXT_COLOR);
            }
            btnBattle.setFont(FSkin.getRelativeBoldFont(20));
            btnBattle.setCommand(VSubmenuConquestMultiverse.this::launchEvent);
            // the opponent's card zooms on click (CardPreview's default); your commander opens commander selection
            for (final java.awt.event.MouseListener l : commanderPreview.getMouseListeners()) {
                commanderPreview.removeMouseListener(l);
            }
            commanderPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            commanderPreview.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (!isAnimating()) {
                        ConquestDesktop.open(EDocID.HOME_CONQUESTCOMMANDERS);
                    }
                }
            });
            opponentPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblShards.setIcon(ConquestSkin.getIcon(FSkinProp.IMG_AETHER_SHARD, 22));
            lblEmblems.setIcon(ConquestSkin.getIcon(FSkinProp.IMG_PW_BADGE_COMMON, 22));
            lblHint.setText(localizer.getMessage("lblMoveHere"));

            add(lblRegion, "growx, h 28px!");
            add(pnlColors, "growx, h 24px!");
            add(lblEvent, "growx, h 40px!, gapbottom 4");
            add(lblOpponent, "growx, h 20px!");
            add(opponentPreview, "grow, push, gapbottom 8");
            add(btnBattle, "growx, h 48px!, gapbottom 8");
            add(commanderPreview, "grow, push");
            add(lblCommander, "growx, h 20px!, gapbottom 6");
            add(lblShards, "split 2, growx, h 24px!");
            add(lblEmblems, "growx, h 24px!");
            add(lblHint, "growx, h 18px!, gaptop 4");
        }

        private void update() {
            if (model == null) {
                return;
            }
            final ConquestLocation loc = model.getCurrentLocation();
            final ConquestRegion region = loc.getRegion();
            lblRegion.setText(region.toString());
            pnlColors.colors = region.getColorSet();
            pnlColors.repaint();
            final ConquestEvent event = loc.getEvent();
            String details = event.getName();
            if (!event.getVariants().isEmpty()) {
                details += " (" + StringUtils.join(event.getVariants(), ", ") + ")";
            }
            lblEvent.setText("<html><div style='text-align:center'>" + escape(details) + "</div></html>");
            final forge.item.PaperCard opponent = event.getAvatarCard();
            lblOpponent.setText(opponent == null ? event.getOpponentName() : opponent.getDisplayName());
            opponentPreview.setCard(opponent);
            final forge.item.PaperCard commander = model.getSelectedCommander().getCard();
            commanderPreview.setCard(commander);
            lblCommander.setText(commander.getDisplayName());
            lblShards.setText(localizer.getMessage("lblAetherShards") + ": " + model.getAEtherShards());
            lblEmblems.setText(localizer.getMessage("lblPlaneswalkEmblems") + ": " + model.getPlaneswalkEmblems());
            btnBattle.setEnabled(!isAnimating());
        }

        private static String escape(final String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0;
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            final BufferedImage space = ConquestSkin.getSpace();
            if (space != null) {
                ConquestDesktop.drawCover(g, space, 0, 0, getWidth(), getHeight());
            }
            g.setColor(ConquestDesktop.BAR_COLOR);
            g.fillRect(0, 0, getWidth(), 100);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1));
            g.drawLine(0, 100, getWidth(), 100);
            super.paintComponent(g0);
        }
    }

    @SuppressWarnings("serial")
    private static final class ColorSetPanel extends JPanel {
        private ColorSet colors;

        private ColorSetPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (colors == null) {
                return;
            }
            final int size = getHeight() - 4;
            final int count = Math.max(1, colors.getOrderedColors().size());
            CardFaceSymbols.drawColorSet(g, colors, (getWidth() - size * count) / 2, 2, size, false);
        }
    }
}
