package forge.game.player;

/** 
 * Used by PlayerController.confirmAction - to avoid a lot of hardcoded strings for mode
 *
 */
public enum PlayerActionConfirmMode {
    Random,
    FromOpeningHand,
    ChangeZoneToAltDestination,
    ChangeZoneFromAltSource,
    ChangeZoneGeneral,
    BidLife,
    OptionalChoose,
    Tribute,
    AlternativeDamageAssignment,
    // casting a card from the library mid-search (reverse miracle): the AI weighs it like casting it now
    CastFromSearch
    // Ripple;
    ;
    
}
