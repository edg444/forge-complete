package forge.card;


public enum CardStateName {
    Original,
    FaceDown,
    Flipped,
    Backside,
    Meld,
    LeftSplit,
    RightSplit,
    // Who // What // When // Where // Why is the only card ever printed with more than two split
    // faces. Two-face splits never see these, so they take exactly the path they always did.
    Split3,
    Split4,
    Split5,
    Secondary,
    PreparedSpell,
    EmptyRoom,
    SpecializeW,
    SpecializeU,
    SpecializeB,
    SpecializeR,
    SpecializeG

    ;

    /** Split faces in printed order. A normal split card only ever occupies the first two. */
    public static final java.util.List<CardStateName> SPLIT_STATES =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                    LeftSplit, RightSplit, Split3, Split4, Split5));

    /**
     * TODO: Write javadoc for this method.
     * @param value
     * @return
     */
    public static CardStateName smartValueOf(String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final CardStateName v : CardStateName.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        if ("Flip".equalsIgnoreCase(value)) {
            return CardStateName.Flipped;
        }
        if ("DoubleFaced".equalsIgnoreCase(value)) {
            return CardStateName.Backside;
        }

        throw new IllegalArgumentException("No element named " + value + " in enum CardCharactersticName");
    }
}
