package forge.toolbox.imaging;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Foil finish for card images: a soft rainbow gradient with a gentle highlight band.
 * Same math as Shaders.fragCardShaderHolo in forge-gui-mobile; keep the two in step.
 * Both blends are masked by luminance so dark ink and rules text are left untouched.
 */
public final class FoilEffect {

    private record Foiled(int foilIndex, BufferedImage image) {}

    // keyed by source image identity; callers pass the cached instances from ImageCache, so this stays small
    private static final Map<BufferedImage, Foiled> CACHE = new WeakHashMap<>();

    private FoilEffect() {}

    /** Returns src unchanged when the card isn't foil or the foil overlay preference is off. */
    public static BufferedImage apply(final BufferedImage src, final int foilIndex) {
        if (src == null || foilIndex <= 0 || !FModel.getPreferences().getPrefBoolean(FPref.UI_OVERLAY_FOIL_EFFECT)) {
            return src;
        }
        synchronized (CACHE) {
            Foiled cached = CACHE.get(src);
            if (cached == null || cached.foilIndex() != foilIndex) {
                cached = new Foiled(foilIndex, render(src, foilIndex));
                CACHE.put(src, cached);
            }
            return cached.image();
        }
    }

    private static BufferedImage render(final BufferedImage src, final int foilIndex) {
        final int w = src.getWidth(), h = src.getHeight();
        final boolean alpha = src.getColorModel().hasAlpha();
        final int[] px = src.getRGB(0, 0, w, h, null, 0, w);

        final float seed = fract(foilIndex * 0.61803399f);
        final float seed2 = fract(foilIndex * 0.75487767f + 0.31f);
        final float ang = 0.45f + seed * 0.6f;
        final float cos = (float) Math.cos(ang), sin = (float) Math.sin(ang);
        final float center = -0.30f + 0.60f * seed2;
        final float[] tint = new float[3];

        for (int y = 0; y < h; y++) {
            final float qy = (y + 0.5f) / h - 0.5f;
            for (int x = 0; x < w; x++) {
                final int i = y * w + x;
                final int p = px[i];
                float r = ((p >> 16) & 0xFF) / 255f, g = ((p >> 8) & 0xFF) / 255f, b = (p & 0xFF) / 255f;

                final float qx = ((x + 0.5f) / w - 0.5f) * w / h;
                final float d = qx * cos + qy * sin;
                final float dc = (d - center) / 0.24f;
                final float band = (float) Math.exp(-dc * dc);
                spectrum(d * 1.1f + seed * 3f, tint);

                final float mask = smoothstep(0.08f, 0.50f, 0.299f * r + 0.587f * g + 0.114f * b);
                final float k = 0.55f * mask;
                r += (r * (0.80f + 0.40f * tint[0]) - r) * k;
                g += (g * (0.80f + 0.40f * tint[1]) - g) * k;
                b += (b * (0.80f + 0.40f * tint[2]) - b) * k;

                final float light = 0.14f * band * mask;
                r = screen(r, (0.4f * tint[0] + 0.6f) * light);
                g = screen(g, (0.4f * tint[1] + 0.6f) * light);
                b = screen(b, (0.4f * tint[2] + 0.6f) * light);

                px[i] = (p & 0xFF000000) | (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
            }
        }
        final BufferedImage out = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    private static void spectrum(final float hue, final float[] out) {
        out[0] = clamp01(Math.abs(fract(hue) * 6f - 3f) - 1f);
        out[1] = clamp01(Math.abs(fract(hue + 0.6667f) * 6f - 3f) - 1f);
        out[2] = clamp01(Math.abs(fract(hue + 0.3333f) * 6f - 3f) - 1f);
    }

    private static float screen(final float base, final float light) {
        return clamp01(1f - (1f - base) * (1f - light));
    }

    private static float smoothstep(final float e0, final float e1, final float x) {
        final float t = clamp01((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }

    private static float fract(final float x) {
        return x - (float) Math.floor(x);
    }

    private static float clamp01(final float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    private static int toByte(final float x) {
        return Math.round(clamp01(x) * 255f);
    }
}
