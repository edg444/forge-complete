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
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.google.common.collect.ImmutableList;

import forge.gamemodes.planarconquest.ConquestPlane;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.util.IterableUtil;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

/** Desktop counterpart of the mobile plane selector: plane art cycling inside a monitor, with arrows to browse. */
@SuppressWarnings("serial")
public class ConquestPlaneSelector extends JPanel {
    private static final Color BACK_COLOR = new Color(1, 2, 2);
    private static final float MONITOR_TOP_MULTIPLIER = 15f / 315f;
    private static final float MONITOR_BOTTOM_MULTIPLIER = 23f / 315f;
    private static final float MONITOR_LEFT_MULTIPLIER = 19f / 443f;
    private static final int NAME_AREA_HEIGHT = 56;

    private List<ConquestPlane> planes = reachablePlanes();
    private final Timer timer;
    private int selectedIndex;
    private int artIndex;
    private int currentPlaneIndex = -1;
    private Rectangle leftArrowBounds = new Rectangle();
    private Rectangle rightArrowBounds = new Rectangle();
    private Rectangle artBounds = new Rectangle();

    public ConquestPlaneSelector() {
        setOpaque(true);
        setBackground(BACK_COLOR);
        timer = new Timer(2500, e -> {
            final FCollectionView<PaperCard> planeCards = getSelectedPlane().getPlaneCards();
            if (++artIndex >= planeCards.size()) {
                artIndex = 0;
            }
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (leftArrowBounds.contains(e.getPoint())) {
                    incrementSelectedIndex(-1);
                } else if (rightArrowBounds.contains(e.getPoint())) {
                    incrementSelectedIndex(1);
                } else if (artBounds.contains(e.getPoint())) {
                    showDescription();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final boolean hot = leftArrowBounds.contains(e.getPoint()) || rightArrowBounds.contains(e.getPoint())
                        || artBounds.contains(e.getPoint());
                setCursor(Cursor.getPredefinedCursor(hot ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        reset();
    }

    private static List<ConquestPlane> reachablePlanes() {
        return ImmutableList.copyOf(IterableUtil.filter(FModel.getPlanes(), plane -> !plane.isUnreachable()));
    }

    public void updateReachablePlanes() {
        planes = reachablePlanes();
    }

    public ConquestPlane getSelectedPlane() {
        return planes.get(selectedIndex);
    }

    /** Hides the given plane (the one you're on) and selects the next one. */
    public void setCurrentPlane(final ConquestPlane plane) {
        currentPlaneIndex = planes.indexOf(plane);
        setSelectedIndex((currentPlaneIndex + 1) % planes.size());
    }

    public void activate() {
        timer.restart();
    }

    public void deactivate() {
        timer.stop();
    }

    public void reset() {
        currentPlaneIndex = -1;
        setSelectedIndex(0);
        timer.stop();
    }

    protected void onSelectionChange() {
    }

    private void setSelectedIndex(final int index) {
        if (index == currentPlaneIndex) {
            return;
        }
        selectedIndex = index;
        artIndex = 0;
        if (timer != null && timer.isRunning()) {
            timer.restart();
        }
        onSelectionChange();
        repaint();
    }

    private void incrementSelectedIndex(final int dir) {
        int newIndex = selectedIndex + dir;
        if (newIndex == currentPlaneIndex && currentPlaneIndex != -1) {
            newIndex += dir;
        }
        if (newIndex < 0) {
            newIndex = planes.size() - 1;
            if (newIndex == currentPlaneIndex) {
                newIndex--;
            }
        } else if (newIndex >= planes.size()) {
            newIndex = 0;
            if (newIndex == currentPlaneIndex) {
                newIndex++;
            }
        }
        setSelectedIndex(newIndex);
    }

    private void showDescription() {
        final ConquestPlane plane = getSelectedPlane();
        final String desc = plane.getDescription();
        final String title = plane.getName().replace("_", " ");
        if (desc.isEmpty()) {
            FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblThisPlaneHasNoDesc"), title);
        } else {
            FOptionPane.showMessageDialog(desc.replace("\\n", "\n"), title);
        }
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        super.paintComponent(g0);
        final Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        final BufferedImage space = ConquestSkin.getSpace();
        if (space != null) {
            final int bgHeight = w * space.getHeight() / space.getWidth();
            g.drawImage(space, 0, h - bgHeight, w, bgHeight, null);
        }

        final BufferedImage monitor = ConquestSkin.getImage(FSkinProp.IMG_PLANE_MONITOR);
        final float monitorAspect = monitor != null ? (float) monitor.getWidth() / monitor.getHeight() : 900f / 650f;
        float monitorWidth = w - 16;
        float monitorHeight = monitorWidth / monitorAspect;
        final float maxMonitorHeight = h - NAME_AREA_HEIGHT - 8;
        if (monitorHeight > maxMonitorHeight) {
            monitorHeight = maxMonitorHeight;
            monitorWidth = monitorHeight * monitorAspect;
        }
        final float monitorLeft = (w - monitorWidth) / 2;
        final float monitorTop = 8;
        final float leftOffset = monitorWidth * MONITOR_LEFT_MULTIPLIER;
        final float topOffset = monitorHeight * MONITOR_TOP_MULTIPLIER;
        final float bottomOffset = monitorHeight * MONITOR_BOTTOM_MULTIPLIER;

        final ConquestPlane plane = getSelectedPlane();
        final FCollectionView<PaperCard> planeCards = plane.getPlaneCards();
        final BufferedImage art = planeCards.isEmpty() ? null
                : ConquestCardArt.getArt(planeCards.get(Math.min(artIndex, planeCards.size() - 1)), false);
        artBounds = new Rectangle(Math.round(monitorLeft + leftOffset), Math.round(monitorTop + topOffset),
                Math.round(monitorWidth - 2 * leftOffset), Math.round(monitorHeight - topOffset - bottomOffset));
        if (art != null) {
            ConquestDesktop.drawCover(g, art, artBounds.x, artBounds.y, artBounds.width, artBounds.height);
        }
        if (monitor != null) {
            g.drawImage(monitor, Math.round(monitorLeft), Math.round(monitorTop), Math.round(monitorWidth), Math.round(monitorHeight), null);
        }

        final int nameTop = Math.round(monitorTop + monitorHeight);
        final int nameHeight = h - nameTop;
        final Font font = FSkin.getRelativeBoldFont(26).getBaseFont();
        g.setFont(font);
        final FontMetrics fm = g.getFontMetrics();
        final String name = plane.getName().replace("_", " ");
        g.setColor(Color.WHITE);
        final int textY = nameTop + (nameHeight - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(name, (w - fm.stringWidth(name)) / 2, textY);

        final int arrowSize = fm.getAscent();
        final int yMid = nameTop + nameHeight / 2;
        final int arrowMargin = Math.round(monitorLeft) + 30;
        g.setStroke(new java.awt.BasicStroke(3f));
        drawArrow(g, arrowMargin, yMid, arrowSize, true);
        drawArrow(g, w - arrowMargin, yMid, arrowSize, false);
        final int hit = arrowSize * 2;
        leftArrowBounds = new Rectangle(arrowMargin - hit, nameTop, hit * 2, nameHeight);
        rightArrowBounds = new Rectangle(w - arrowMargin - hit, nameTop, hit * 2, nameHeight);
    }

    private static void drawArrow(final Graphics2D g, final int xMid, final int yMid, final int size, final boolean left) {
        final int dx = size / 4;
        final int dy = size / 2;
        if (left) {
            g.drawLine(xMid + dx, yMid - dy, xMid - dx, yMid);
            g.drawLine(xMid - dx, yMid, xMid + dx, yMid + dy);
        } else {
            g.drawLine(xMid - dx, yMid - dy, xMid + dx, yMid);
            g.drawLine(xMid + dx, yMid, xMid - dx, yMid + dy);
        }
    }
}
