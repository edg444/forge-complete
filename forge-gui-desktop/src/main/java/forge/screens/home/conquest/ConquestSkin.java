package forge.screens.home.conquest;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;

/**
 * The desktop skin never loads the Planar Conquest sprite or the space and Chaos Wheel
 * backgrounds (only the mobile UI used them), so this reads them straight from the default skin.
 */
public final class ConquestSkin {
    private static BufferedImage sprite;
    private static BufferedImage space;
    private static BufferedImage chaosWheel;
    private static final Map<FSkinProp, BufferedImage> images = new EnumMap<>(FSkinProp.class);
    private static final Map<String, SkinImage> icons = new HashMap<>();

    private ConquestSkin() {
    }

    private static BufferedImage read(final String file) {
        try {
            return ImageIO.read(new File(ForgeConstants.DEFAULT_SKINS_DIR + file));
        } catch (final Exception e) {
            System.err.println("Planar Conquest: could not read skin file " + file + ": " + e.getMessage());
            return null;
        }
    }

    /** A Planar Conquest sprite image, or a regular skin image for any other prop. */
    public static BufferedImage getImage(final FSkinProp prop) {
        if (prop.getType() != FSkinProp.PropType.PLANAR_CONQUEST) {
            final Image img = FSkin.getImage(prop).getIcon().getImage();
            if (img instanceof BufferedImage bi) {
                return bi;
            }
            final BufferedImage bi = new BufferedImage(Math.max(1, img.getWidth(null)), Math.max(1, img.getHeight(null)), BufferedImage.TYPE_INT_ARGB);
            bi.getGraphics().drawImage(img, 0, 0, null);
            return bi;
        }
        BufferedImage image = images.get(prop);
        if (image == null) {
            if (sprite == null) {
                sprite = read(ForgeConstants.SPRITE_PLANAR_CONQUEST_FILE);
                if (sprite == null) {
                    return null;
                }
            }
            final int[] c = prop.getCoords();
            image = sprite.getSubimage(c[0], c[1], c[2], c[3]);
            images.put(prop, image);
        }
        return image;
    }

    /** A skin image of the given prop scaled to a square of the given size, for labels and buttons. */
    public static SkinImage getIcon(final FSkinProp prop, final int size) {
        final String key = prop.name() + "#" + size;
        SkinImage icon = icons.get(key);
        if (icon == null) {
            final BufferedImage image = getImage(prop);
            if (image == null) {
                return FSkin.getImage(FSkinProp.ICO_UNKNOWN);
            }
            icon = SkinImage.fromImage(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
            icons.put(key, icon);
        }
        return icon;
    }

    public static BufferedImage getSpace() {
        if (space == null) {
            space = read(ForgeConstants.SPACE_BG_FILE);
        }
        return space;
    }

    public static BufferedImage getChaosWheel() {
        if (chaosWheel == null) {
            chaosWheel = read(ForgeConstants.CHAOS_WHEEL_IMG_FILE);
        }
        return chaosWheel;
    }

    public static BufferedImage getBadge(final int tier) {
        return getImage(switch (tier) {
            case 0 -> FSkinProp.IMG_PW_BADGE_COMMON;
            case 1 -> FSkinProp.IMG_PW_BADGE_UNCOMMON;
            case 2 -> FSkinProp.IMG_PW_BADGE_RARE;
            default -> FSkinProp.IMG_PW_BADGE_MYTHIC;
        });
    }
}
