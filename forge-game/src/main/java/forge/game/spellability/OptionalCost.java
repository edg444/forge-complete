package forge.game.spellability;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum OptionalCost {
    Bargain("Bargain", ""),
    Buyback("Buyback", ""),
    Entwine("Entwine", ""),
    Kicker1("Kicker", ""),
    Kicker2("Kicker", ""),
    PromiseGift("Promise Gift", ""),
    Retrace("Retrace", ""),
    Jumpstart("Jump-start", ""),
    Offering("Offering", ""),
    Teamwork("Teamwork", ""),
    ReduceW("(to reduce white mana)", "W"),
    ReduceU("(to reduce blue mana)", "U"),
    ReduceB("(to reduce black mana)", "B"),
    ReduceR("(to reduce red mana)", "R"),
    ReduceG("(to reduce green mana)", "G"),
    AltCost("", ""),
    Flash("Flash", ""), // used for Pay Extra for Flash
    // Wicker Picker's sticker kicker, one per instance (each Wicker Picker can be paid for once per spell)
    StickerKicker1("Sticker kicker", ""),
    StickerKicker2("Sticker kicker", ""),
    StickerKicker3("Sticker kicker", ""),
    StickerKicker4("Sticker kicker", ""),
    Generic("Generic", ""); // used by "Dragon Presence" and pseudo-kicker cards

    private String name;
    private String pip;
    
    OptionalCost(String name, String pip) {
        this.name = name;
        this.pip = pip;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the pip
     */
    public String getPip() {
        return pip;
    }
}
