package forge.card;

import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 *
 */
public interface ICardFace extends ICardCharacteristics, ICardRawAbilites, Comparable<ICardFace> {
    /** True for a printed stat with a trailing half, e.g. Unhinged's "1.5" or ".5". */
    static boolean isHalfPT(final String val) {
        return val != null && val.endsWith(".5");
    }

    String getFlavorName();

    /**
     * @return this card's flavor name if it has one. Otherwise, the card's Oracle name.
     */
    default String getDisplayName() {
        if (this.getFlavorName() != null)
            return this.getFlavorName();
        return this.getName();
    }

    boolean hasFunctionalVariants();
    ICardFace getFunctionalVariant(String variant);
    Map<String, ? extends ICardFace> getFunctionalVariants();
}
