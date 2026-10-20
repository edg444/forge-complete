package forge.assets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;

/**
 * A mana symbol with no art anywhere in the skin sprites, drawn at load time instead.
 * <p>
 * Two things need this: pink, which is only ever a card's color and so was never given a symbol,
 * and generic costs past {20}, where the sprite sheet simply stops. Both would otherwise reach
 * CardFaceSymbols with no image and raise a bug report on the phone.
 */
public class FDrawnSymbol implements FSkinImageInterface {
    private static final int SIZE = 64;

    private final Color fill, rim;
    private final Color[] wedges;
    private final boolean acorn;
    private TextureRegion textureRegion;

    public FDrawnSymbol(Color fill0, Color rim0) {
        fill = fill0;
        rim = rim0;
        wedges = null;
        acorn = false;
    }

    /** A disc split into equal wedges clockwise from the top, one per color. */
    public FDrawnSymbol(Color[] wedges0, Color rim0) {
        fill = wedges0[0];
        rim = rim0;
        wedges = wedges0;
        acorn = false;
    }

    private FDrawnSymbol() {
        fill = new Color(0.769f, 0.502f, 0.22f, 1f);
        rim = new Color(0.471f, 0.306f, 0.141f, 1f);
        wedges = null;
        acorn = true;
    }

    /** Acornelia's {A} (an acorn counter): an acorn on a clear ground, as on the desktop. */
    public static FDrawnSymbol acorn() {
        return new FDrawnSymbol();
    }

    private void drawAcorn(final Pixmap pixmap) {
        final Color outline = new Color(0.275f, 0.157f, 0.059f, 1f);
        // the nut, a rounded oval below the cap
        pixmap.setColor(outline);
        pixmap.fillCircle(SIZE / 2, 40, 17);
        pixmap.fillCircle(SIZE / 2, 46, 15);
        pixmap.setColor(fill);
        pixmap.fillCircle(SIZE / 2, 40, 15);
        pixmap.fillCircle(SIZE / 2, 46, 13);
        // the cap, a dome with a flat rim over the top of the nut
        pixmap.setColor(outline);
        pixmap.fillCircle(SIZE / 2, 26, 22);
        pixmap.setColor(rim);
        pixmap.fillCircle(SIZE / 2, 26, 20);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.fillRectangle(0, 31, SIZE, 18);
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(outline);
        pixmap.fillRectangle(9, 27, SIZE - 18, 6);
        pixmap.setColor(rim);
        pixmap.fillRectangle(11, 28, SIZE - 22, 3);
        // the nut again where clearing the cap's lower half cut it
        pixmap.setColor(outline);
        pixmap.fillCircle(SIZE / 2, 46, 15);
        pixmap.fillRectangle(SIZE / 2 - 17, 33, 34, 13);
        pixmap.setColor(fill);
        pixmap.fillCircle(SIZE / 2, 46, 13);
        pixmap.fillRectangle(SIZE / 2 - 15, 33, 30, 13);
        // the stem
        pixmap.setColor(outline);
        pixmap.fillRectangle(SIZE / 2 - 2, 0, 5, 8);
    }

    private TextureRegion region() {
        if (textureRegion == null) {
            final Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            if (acorn) {
                drawAcorn(pixmap);
                return finish(pixmap);
            }
            pixmap.setColor(fill);
            pixmap.fillCircle(SIZE / 2, SIZE / 2, SIZE / 2 - 2);
            if (wedges != null) {
                final float r = SIZE / 2f - 2;
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        final float dx = x + 0.5f - SIZE / 2f, dy = y + 0.5f - SIZE / 2f;
                        if (dx * dx + dy * dy > r * r) {
                            continue;
                        }
                        // clockwise from 12 o'clock, y growing downward
                        double angle = Math.toDegrees(Math.atan2(dx, -dy));
                        if (angle < 0) {
                            angle += 360;
                        }
                        pixmap.setColor(wedges[Math.min(wedges.length - 1, (int) (angle / (360.0 / wedges.length)))]);
                        pixmap.drawPixel(x, y);
                    }
                }
            }
            pixmap.setColor(rim);
            pixmap.drawCircle(SIZE / 2, SIZE / 2, SIZE / 2 - 2);
            return finish(pixmap);
        }
        return textureRegion;
    }

    private TextureRegion finish(final Pixmap pixmap) {
        final Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureRegion = new TextureRegion(texture);
        pixmap.dispose();
        return textureRegion;
    }

    @Override
    public void load(Pixmap preferredIcons) {
        region();
    }

    @Override
    public TextureRegion getTextureRegion() {
        return region();
    }

    @Override
    public float getNearestHQWidth(float baseWidth) {
        return baseWidth;
    }

    @Override
    public float getNearestHQHeight(float baseHeight) {
        return baseHeight;
    }

    @Override
    public float getWidth() {
        return SIZE;
    }

    @Override
    public float getHeight() {
        return SIZE;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        g.drawImage(region(), x, y, w, h);
    }
}
