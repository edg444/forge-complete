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
    private TextureRegion textureRegion;

    public FDrawnSymbol(Color fill0, Color rim0) {
        fill = fill0;
        rim = rim0;
    }

    private TextureRegion region() {
        if (textureRegion == null) {
            final Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            pixmap.setColor(fill);
            pixmap.fillCircle(SIZE / 2, SIZE / 2, SIZE / 2 - 2);
            pixmap.setColor(rim);
            pixmap.drawCircle(SIZE / 2, SIZE / 2, SIZE / 2 - 2);
            final Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textureRegion = new TextureRegion(texture);
            pixmap.dispose();
        }
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
