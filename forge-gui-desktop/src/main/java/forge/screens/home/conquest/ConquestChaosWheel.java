package forge.screens.home.conquest;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import forge.gamemodes.planarconquest.ConquestEvent.ChaosWheelOutcome;
import forge.localinstance.skin.FSkinProp;
import forge.util.Aggregates;
import forge.util.Localizer;
import forge.view.FDialog;

/** Spins the Chaos Wheel with the same physics as on mobile; modal, returns the outcome it lands on. */
@SuppressWarnings("serial")
public final class ConquestChaosWheel extends FDialog {
    private static final int TICK_MS = 15;
    private static final float WAIT_DURATION = 1f;
    private static final int WHEEL_SIZE = 460;

    private final Timer timer;
    private float position;
    private float velocity;
    private float acceleration;
    private boolean doneSpinning;
    private float waited;

    public static ChaosWheelOutcome spin() {
        final ConquestChaosWheel wheel = new ConquestChaosWheel();
        wheel.setVisible(true);
        wheel.dispose();
        return ChaosWheelOutcome.getWheelOutcome(wheel.getWheelRotation());
    }

    private ConquestChaosWheel() {
        super(true, false, "10");
        setTitle(Localizer.getInstance().getMessage("lblPlanarConquest"));
        // -22.5 because the wheel image starts slightly rotated
        position = Aggregates.randomInt(1, 8) * 45f - 22.5f;
        velocity = Aggregates.randomInt(360, 720);
        acceleration = -Aggregates.randomInt(50, 100);
        final WheelPanel panel = new WheelPanel();
        add(panel, "w " + WHEEL_SIZE + "px!, h " + Math.round(WHEEL_SIZE * 1.12f) + "px!");
        pack();
        timer = new Timer(TICK_MS, e -> advance(TICK_MS / 1000f));
        timer.start();
    }

    // negative so the wheel turns clockwise
    private float getWheelRotation() {
        return -position;
    }

    private void advance(final float dt) {
        if (!doneSpinning) {
            final float sign = Math.signum(velocity);
            position += (velocity + 0.5f * acceleration * dt) * dt;
            velocity += acceleration * dt;
            if (Math.signum(velocity) != sign) {
                velocity = 0;
                doneSpinning = true;
            }
            while (position > 360f) {
                position -= 360f;
            }
            repaint();
        } else {
            waited += dt;
            if (waited >= WAIT_DURATION) {
                timer.stop();
                setVisible(false);
            }
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        // the wheel can't be dismissed early: the spin decides the reward
        if (!visible && timer != null && timer.isRunning()) {
            return;
        }
        super.setVisible(visible);
    }

    private final class WheelPanel extends JPanel {
        private WheelPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(final Graphics g0) {
            super.paintComponent(g0);
            final Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int size = Math.min(getWidth(), Math.round(getHeight() / 1.12f));
            final int x = (getWidth() - size) / 2;
            final int spokeSize = Math.round(size * 0.15f);
            final int y = getHeight() - size;
            final BufferedImage wheel = ConquestSkin.getChaosWheel();
            if (wheel != null) {
                final AffineTransform old = g.getTransform();
                // libGDX turns positive angles counterclockwise and Java2D clockwise, hence the sign flip
                g.rotate(Math.toRadians(-getWheelRotation()), x + size / 2.0, y + size / 2.0);
                g.drawImage(wheel, x, y, size, size, null);
                g.setTransform(old);
            }
            final BufferedImage spoke = ConquestSkin.getImage(FSkinProp.IMG_PW_BADGE_UNCOMMON);
            if (spoke != null) {
                g.drawImage(spoke, x + (size - spokeSize) / 2, Math.max(0, y - Math.round(spokeSize * 0.75f)), spokeSize, spokeSize, null);
            }
            g.dispose();
        }
    }
}
