package forge.game.event;

/**
 * How a thrown object is drawn by the throw animation (GameEventFlipOntoBattlefield) - fork additions for the funny
 * cards that throw something other than a whole card onto the table.
 *
 * @param piece        a torn-off quarter of the card (0-3, Chaos Confetti), or -1 for the whole card
 * @param dieFace      a die showing this face instead of a card (Ol' Buzzbark), or 0
 * @param keepHits     the game already decided what it touches; the board mustn't pick its own hits
 * @param returnHome   it comes back down on its own spot (Clay Pigeon, Orcish Paratroopers)
 * @param landFaceDown it lands face down (Orcish Paratroopers' bad drop)
 */
public record TossStyle(int piece, int dieFace, boolean keepHits, boolean returnHome, boolean landFaceDown) implements java.io.Serializable {
    public static final TossStyle CARD = new TossStyle(-1, 0, false, false, false);

    public static TossStyle piece(final int piece) {
        return new TossStyle(piece, 0, false, false, false);
    }

    public static TossStyle die(final int face) {
        return new TossStyle(-1, face, true, false, false);
    }

    public static TossStyle inPlace(final boolean landFaceDown) {
        return new TossStyle(-1, 0, true, true, landFaceDown);
    }
}
