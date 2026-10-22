/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.PrintingTraits;
import forge.item.PaperCard;

/**
 * {@code DeckRule:ColorIdentity:Exempt$ <branches> | Disable$ True | AllowedAdditionalColor$
 * <n> | AllowedAdditionalColorType$ <branches> | ExemptUniversesBeyond$ True} - exempts matching cards from the deck's
 * color-identity check, disables it entirely, or lets the player choose (Cryptic Spires-style,
 * via the deck editor) up to {@code n} extra colors for cards matching a branch to use. ExemptUniversesBeyond$
 * (Byode, Inverse Sun) exempts Universes Beyond printings, which only the printing knows. Branch
 * grammar/matching: {@link CardRulesPredicates#restrictionList}.
 */
public class DeckRuleColorIdentity extends DeckRule {
    private final Predicate<CardRules> exemptPredicate;
    private final boolean disabled;
    private final boolean exemptUniversesBeyond;
    private final Predicate<CardRules> additionalColorPredicate;
    private final int additionalColorCount;
    private final byte allowedAdditionalColors;
    private final List<Byte> colorPairs = new java.util.ArrayList<>();

    DeckRuleColorIdentity(final Map<String, String> params, final byte chosenAdditionalColors) {
        super(params);
        disabled = "True".equalsIgnoreCase(params.get("Disable"));
        exemptUniversesBeyond = "True".equalsIgnoreCase(params.get("ExemptUniversesBeyond"));
        final String exempt = params.get("Exempt");
        exemptPredicate = exempt != null ? CardRulesPredicates.restrictionList(exempt) : card -> false;
        final String additionalColor = params.get("AllowedAdditionalColor");
        additionalColorCount = additionalColor != null ? Integer.parseInt(additionalColor.trim()) : 0;
        final String additionalColorType = params.get("AllowedAdditionalColorType");
        additionalColorPredicate = additionalColorCount > 0 && additionalColorType != null
                ? CardRulesPredicates.restrictionList(additionalColorType) : card -> false;
        // The Strixhaven-Lorwyn Rover: ColorIdentityOneOf$ WR,UR,GU,WB,BG - the commander has one of these color
        // identities, whichever the deck fits
        final String oneOf = params.get("ColorIdentityOneOf");
        if (oneOf != null) {
            for (final String colors : oneOf.split(",")) {
                colorPairs.add(ColorSet.fromNames(colors.trim().toCharArray()).getColor());
            }
        }
        // Ignore a stored selection that doesn't fit the budget (e.g. a hand-edited decklist).
        allowedAdditionalColors = ColorSet.fromMask(chosenAdditionalColors).countColors() <= additionalColorCount
                ? chosenAdditionalColors : 0;
    }

    /** True if color identity checking is waived entirely for cards this rule applies to (The Paradise Bird-style). */
    public boolean disablesColorIdentityCheck() {
        return disabled;
    }

    /** True if the candidate matches at least one {@code Exempt$} branch (or the check is fully disabled). */
    public boolean allowsOffColorIdentity(final CardRules candidate) {
        return disabled || exemptPredicate.test(candidate);
    }

    /** Like {@link #allowsOffColorIdentity(CardRules)}, plus the exemptions that depend on the printing. */
    public boolean allowsOffColorIdentity(final PaperCard candidate) {
        return allowsOffColorIdentity(candidate.getRules()) || exemptUniversesBeyond
                && PrintingTraits.isUniversesBeyond(candidate.getEdition(), candidate.getCollectorNumber());
    }

    /** The ColorIdentityOneOf$ choices, as color masks (empty if this rule has none). */
    public List<Byte> getColorIdentityChoices() {
        return colorPairs;
    }

    /** True if this rule lets the player choose any AllowedAdditionalColor$ colors at all. */
    public boolean hasAllowedAdditionalColorBudget() {
        return additionalColorCount > 0;
    }

    /** How many extra colors the player may choose for this rule's AllowedAdditionalColor$ budget. */
    public int getAdditionalColorCount() {
        return additionalColorCount;
    }

    /** The colors the player has chosen for this rule's AllowedAdditionalColor$ budget (0 if none chosen yet). */
    public byte getAllowedAdditionalColors() {
        return allowedAdditionalColors;
    }

    /** True if candidate matches this rule's branch and fits within commanderCI plus the chosen additional colors. */
    public boolean approvesAdditionalColor(final CardRules candidate, final byte commanderCI) {
        if (additionalColorCount == 0 || !additionalColorPredicate.test(candidate)) {
            return false;
        }
        return candidate.getColorIdentity().hasNoColorsExcept(commanderCI | allowedAdditionalColors);
    }
}
