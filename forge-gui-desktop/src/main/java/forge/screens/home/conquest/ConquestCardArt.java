package forge.screens.home.conquest;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import forge.ImageCache;
import forge.card.CardEdition;
import forge.card.CardTypeView;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Crops the art box out of a full card image, using the same proportions as the mobile UI so
 * Planar Conquest region and plane art frames the same way on both.
 */
public final class ConquestCardArt {
    public static final float CARD_ART_RATIO = 1.302f;
    private static final float CARD_ART_HEIGHT_PERCENTAGE = 0.43f;
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    private ConquestCardArt() {
    }

    /** The card's art, or null while its image isn't available (it isn't cached then, so it's retried). */
    public static BufferedImage getArt(final PaperCard card, final boolean backFace) {
        if (card == null) {
            return null;
        }
        final String key = card.getImageKey(backFace);
        BufferedImage art = cache.get(key);
        if (art != null) {
            return art;
        }
        final BufferedImage image = ImageCache.getCardOriginalImageInfo(key, false).getLeft();
        if (image == null || ImageCache.isDefaultImage(image)) {
            return null;
        }
        art = crop(image, card);
        cache.put(key, art);
        return art;
    }

    private static BufferedImage crop(final BufferedImage image, final PaperCard card) {
        final CardTypeView type = card.getRules().getType();
        float w = image.getWidth();
        float h = image.getHeight();
        float x;
        float y;
        if (type.isPlane() || type.isPhenomenon()) {
            if (w > h) {
                x = w * 40f / 430f;
                y = h * 40f / 300f;
                w *= 350f / 430f;
                h *= 156f / 300f;
            } else {
                // a portrait scan of a horizontal card: crop the same box, then turn it upright
                x = w * 40f / 300f;
                y = h * (430f - 350f - 40f) / 430f;
                w *= 156f / 300f;
                h *= 350f / 430f;
                return rotateClockwise(sub(image, x, y, w, h));
            }
        } else if (type.isPlaneswalker()) {
            x = w * 0.09f;
            y = h * 0.11f;
            w -= 2f * x;
            h -= 5.71f * y;
        } else {
            final boolean modern = isModernFrame(card);
            x = modern ? w * 0.1f : w * 0.12f;
            y = modern ? h * 0.12f : h * 0.11f;
            w -= modern ? 2 * x : 2.1f * x;
            h *= CARD_ART_HEIGHT_PERCENTAGE;
            final float ratioRatio = w / h / CARD_ART_RATIO;
            if (ratioRatio > 1) {
                final float dw = w * (ratioRatio - 1);
                w -= dw;
                x += dw / 2;
            } else {
                final float dh = h * (1 - ratioRatio);
                h -= dh;
                y += dh / 2;
            }
        }
        return sub(image, x, y, w, h);
    }

    private static BufferedImage sub(final BufferedImage image, final float x, final float y, final float w, final float h) {
        final int ix = Math.max(0, Math.round(x));
        final int iy = Math.max(0, Math.round(y));
        final int iw = Math.max(1, Math.min(Math.round(w), image.getWidth() - ix));
        final int ih = Math.max(1, Math.min(Math.round(h), image.getHeight() - iy));
        return image.getSubimage(ix, iy, iw, ih);
    }

    private static BufferedImage rotateClockwise(final BufferedImage src) {
        final BufferedImage out = new BufferedImage(src.getHeight(), src.getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int sx = 0; sx < src.getWidth(); sx++) {
            for (int sy = 0; sy < src.getHeight(); sy++) {
                out.setRGB(src.getHeight() - 1 - sy, sx, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    private static boolean isModernFrame(final PaperCard card) {
        final CardEdition ed = FModel.getMagicDb().getEditions().get(card.getEdition());
        if (ed == null) {
            return false;
        }
        return switch (ed.getCode()) {
            case "MED", "ME2", "ME3", "ME4", "TSB" -> false;
            default -> ed.isModern();
        };
    }
}
