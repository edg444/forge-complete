package forge.game.staticability;

import java.util.EnumSet;
import java.util.Set;

public enum StaticAbilityMode {
    Continuous,
    // Graveyard Busybody - read by Game.updateGraveyardHolder, not applied in layers
    AllGraveyardsYours,
    // Socketed Sprocketer - its installed results may stand in for a die its controller rolls (RollDiceEffect)
    UseInstalledResult,
    // Wall of Fortune - lets its controller tap an untapped Wall to have a player reroll a die (RollDiceEffect)
    RerollWithWall,
    // Snickering Squirrel - lets its controller tap it to increase any player's die result by 1 (RollDiceEffect)
    TapToIncreaseRoll,
    // Squirrel-Powered Scheme - Amount$ added to each die result a ValidPlayer$ rolls (RollDiceEffect)
    IncreaseRollResult,
    // Krark's Other Thumb - a ValidPlayer$ who'd roll a die rolls two and ignores one (RollDiceEffect, PlanarDice)
    RollTwoKeepOne,
    // Far Out - a ValidPlayer$ may choose one or more modes rather than the indicated number (CharmEffect)
    ChooseAnyNumberOfModes,
    // Solaflora - ValidAttachment$ attached to the host affects Affected$ as though attached to them (CardProperty
    // EnchantedBy/EquippedBy/AttachedBy), without their being enchanted or equipped
    ShareAttachments,
    CountersAreTokens,
    TokensAreCards,
    PoisonTolerance,
    // The Pleasant Taxer - see StaticAbilityActionTax
    ActionTax,
    ManaPaymentTax,
    InvertPTCounters,
    JumpStartNotExiled,
    SpeedUncapped,
    CantRemoveCounter,

    // StaticAbility
    CantAttackUnless,
    CantBlockUnless,
    OptionalAttackCost,

    // GameActionUtil
    OptionalCost,

    // StaticAbilityAlternativeCost
    AlternativeCost,

    // StaticAbilityCantBeCast
    CantBeCast,
    CantBeActivated,
    CantPlayLand,
    // StaticAbilityDisableTriggers
    DisableTriggers,

    // StaticAbilityPanharmonicon
    Panharmonicon,

    // StaticAbilityMustTarget
    MustTarget,

    // StaticAbilityPlayerType
    PlayerType,

    // CountsAsCreatures (Five Kids in a Trenchcoat)
    CountsAsCreatures,

    // StaticAbilityCastAs (Form of the Mulldrifter)
    CastAs,

    // CombatDamageOnStack (Stack of Paperwork)
    CombatDamageUsesStack,

    // StaticAbilityCantAttackBlock
    CantAttack,
    CanAttackDefender,
    CantBlock,
    CantBlockBy,
    CanAttackIfHaste,
    CanBlockIfReach,
    CanBlockIfShadow,
    MinMaxBlocker,
    BlockTapped,
    AttackVigilance,

    // StaticAbilityMustAttack
    MustAttack,
    PlayerMustAttack,
    // StaticAbilityAttacksItsController
    AttacksItsController,
    // StaticAbilityAttackDuringOpponentsTurn
    AttackDuringOpponentsTurn,
    // StaticAbilityMustBlock
    MustBlock,

    // StaticAbilityAssignCombatDamageAsUnblocked
    AssignCombatDamageAsUnblocked,

    // StaticAbilityCombatDamageToughness
    CombatDamageToughness,
    CombatDamageNegatePower,

    // StaticAbilityColorlessDamageSource
    ColorlessDamageSource,

    // StaticAbilityNoCleanupDamage
    NoCleanupDamage,

    // StaticAbilityBlockRestrict
    BlockRestrict,

    // StaticAbilityCantGainLosePayLife
    CantGainLife,
    CantLoseLife,
    CantChangeLife,
    CantPayLife,

    // CostAdjustment
    RaiseCost,
    ReduceCost,
    SetCost,

    // StaticAbilityIgnoreHexproofShroud
    IgnoreHexproof,
    IgnoreShroud,

    // StaticAbilityAttackRestrict
    AttackRestrict,

    // StaticAbilityAssignNoCombatDamage
    AssignNoCombatDamage,

    // StaticAbilityAdapt
    CanAdapt,

    // StaticAbilityCantBeCopied
    CantBeCopied,

    // StaticAbilityCantBeBeamedUp
    CantBeBeamedUp,

    // StaticAbilityCantBeSuspected
    CantBeSuspected,

    // StaticAbilityCantBecomeMonarch
    CantBecomeMonarch,

    // StaticAbilityCantGainControl
    CantGainControl,

    // StaticAbilityCantAttach
    CantAttach,

    // StaticAbilityCantCrew
    CantCrew,

    // StaticAbilityCantDraw
    CantDraw,

    // StaticAbilityDrawFromBottom
    DrawFromBottom,

    // StaticAbilityCantDiscard
    CantDiscard,

    // StaticAbilityCantExile
    CantExile,
    // StaticAbilityCantDestroy (A Good Thing)
    CantDestroy,
    // Abian, Luvion Usurper - as it enters, its controller becomes it
    ControllerBecomesThis,

    // StaticAbilityCantPhase
    CantPhaseIn,
    CantPhaseOut,

    // StaticAbilityCantPreventDamage
    CantPreventDamage,

    // StaticAbilityCantPutCounter
    CantPutCounter,

    // StaticAbilityCantRegenerate
    CantRegenerate,

    // StaticAbilityCantSacrifice
    CantSacrifice,

    // StaticAbilityCantTarget
    CantTarget,

    // StaticAbilityCantTransform
    CantTransform,

    // StaticAbilityCantVenture
    CantVenture,

    // StaticAbilityCantChangeDayTime
    CantChangeDayTime,

    // StaticAbilityActivateAbilityAsIfHaste
    ActivateAbilityAsIfHaste,
    // StaticAbilityActivateAsController - The Fearsome Flock
    ActivateAsController,

    // StaticAbilityCastWithFlash
    CastWithFlash,

    // StaticAbilityIgnoreLandwalk
    IgnoreLandwalk,
    // StaticAbilityIgnoreLegendRule
    IgnoreLegendRule,
    IgnorePlaneswalkerZeroLoyaltyRule,
    // StaticAbilityIgnoreStateBasedActions
    IgnoreStateBasedActions,
    // StaticAbilityRandomTargets (High Troller) - targets of ValidSA$ spells and abilities are chosen at random
    RandomTargets,
    // StaticAbilityStackFirstInFirstOut (Queue of Beetles) - the bottom object of the stack resolves first
    StackFirstInFirstOut,
    // Pippa, Duchess of Dice - after any die roll, pay Cost$ to reroll it (RollDiceEffect.payToRerollAnyDie)
    RerollAnyDie,
    // Throat Wolf - after each opponent's first combat phase of each turn, its controller has a combat phase
    CombatAfterOpponentsFirst,
    // Chronobot - its controller and the remembered player switch upkeep steps
    SwitchUpkeep,
    // StaticAbilityDoubleDoubling (Maro's Gone Nuts) - effects that double multiply by twice as much
    DoubleDoubling,

    // StaticAbilityMaxCounter
    MaxCounter,

    // StaticAbilityLifelinkDamage (Syr Cadian) - damage from ValidSource$ also gains its controller that much life
    LifelinkDamage,
    // StaticAbilityInfectDamage
    InfectDamage,
    // StaticAbilityWitherDamage
    WitherDamage,

    // StaticAbilityFlipCoinMod
    FlipCoinMod,
    FlipCoinDoubler,

    // StaticAbilityPlotZone
    PlotZone,

    // StaticAbilityNumLoyaltyAct
    NumLoyaltyAct,

    // StaticAbilityAdditionalActivations
    Activations,

    // StaticAbilityDevotion
    Devotion,
    // StaticAbilityGainLifeRadiation
    GainLifeRadiation,

    // StaticAbilitySurveilNum
    SurveilNum,

    // StaticAbilityTapPowerValue
    TapPowerValue,

    // StaticAbilityStayingPower
    StayingPower,

    // StaticAbilityUnspentMana
    UnspentMana,
    ManaBurn,

    // StaticAbilityManaConvert
    ManaConvert,

    // StaticAbilityUntapOtherPlayer
    UntapOtherPlayer,

    // StaticAbilityTurnPhaseReversed
    TurnReversed,
    PhaseReversed,

    // StaticAbilityAttackRequirement
    AttackRequirement,

    // StaticAbilityCountersRemain
    CountersRemain,

    // StaticAbilityTopLibraryOnBattlefield
    TopLibraryPermanentsOnBattlefield,

    // StaticAbilityGraveyardCombat
    GraveyardCombat,
    // StaticAbilityPlaceInProgram
    PlaceInProgram,

    // StaticAbilityChoosesTargets
    ChoosesTargets,

    // StaticAbilityIgnoreErrata
    IgnoreErrata,

    // StaticAbilityManaRestriction
    ManaRestriction,
    ;

    public static StaticAbilityMode smartValueOf(final String value) {
        if (value == null) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final StaticAbilityMode v : StaticAbilityMode.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum StaticAbilityMode");
    }

    public static Set<StaticAbilityMode> setValueOf(final String values) {
        final Set<StaticAbilityMode> result = EnumSet.noneOf(StaticAbilityMode.class);
        for (final String s : values.split("[, ]+")) {
            StaticAbilityMode zt = StaticAbilityMode.smartValueOf(s);
            if (zt != null) {
                result.add(zt);
            }
        }
        return result;
    }
}
