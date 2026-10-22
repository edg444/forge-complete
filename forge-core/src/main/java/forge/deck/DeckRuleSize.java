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

import java.util.Map;

/**
 * {@code DeckRule:Size:AdjustMax$ <Unlimited|±n> | ActiveSection$ Commander} - lets a card change
 * the deck's max size. {@code AdjustMin$}/{@code Cumulative$} not implemented.
 */
public class DeckRuleSize extends DeckRule {
    private static final String UNLIMITED = "Unlimited";

    private final boolean unlimited;
    private final int maxDelta;
    // The Zonian Brawler: SetMin$ <n>, the deck's whole minimum size, its commander included
    private final int setMin;

    DeckRuleSize(final Map<String, String> params) {
        super(params);
        setMin = params.containsKey("SetMin") ? Integer.parseInt(params.get("SetMin")) : 0;
        final String adjustMax = params.get("AdjustMax");
        if (UNLIMITED.equalsIgnoreCase(adjustMax)) {
            unlimited = true;
            maxDelta = 0;
        } else {
            unlimited = false;
            maxDelta = adjustMax != null ? Integer.parseInt(adjustMax) : 0;
        }
    }

    /** The deck's minimum size with its commander, if this rule sets one (0 if not). */
    public int getSetMin() {
        return setMin;
    }

    /** True if this rule removes the deck's maximum size cap entirely. */
    public boolean removesMaxDeckSize() {
        return unlimited;
    }

    /** The relative change to the deck's maximum size, if any (ignored when {@link #removesMaxDeckSize()} is true). */
    public int getMaxDelta() {
        return maxDelta;
    }
}
