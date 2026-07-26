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
package forge.game.zone;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;
import java.util.function.Predicate;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayerZone extends Zone {
    private static final long serialVersionUID = -5687652485777639176L;

    // the this is not the owner of the card
    private static Predicate<Card> alienCardsActivationFilter(final Player who) {
        return c -> !c.mayPlay(who).isEmpty() || c.mayPlayerLook(who);
    }

    private final class OwnCardsActivationFilter implements Predicate<Card> {
        @Override
        public boolean test(final Card c) {
            if (c.mayPlayerLook(c.getController())) {
                return true;
            }

            if (!c.mayPlay(c.getController()).isEmpty()) {
                return true;
            }

            // Keywords like Flashback/Escape create alternative SAs at play time,
            // not stored on the card or in the mayPlay map. Check directly.
            if (PlayerZone.this.is(ZoneType.Graveyard) && (c.hasKeyword(Keyword.FLASHBACK)
                    || c.hasKeyword(Keyword.RETRACE) || c.hasKeyword(Keyword.JUMP_START)
                    || c.hasKeyword(Keyword.ESCAPE) || c.hasKeyword(Keyword.DISTURB))) {
                return true;
            }
            if (PlayerZone.this.is(ZoneType.Exile) && (c.isForetold() || c.isOnAdventure())) {
                return true;
            }

            return PlayerZone.this.activatableFromHere(c);
        }
    }

    /** Whether any of this card's abilities name this zone, as either their main or an extra one. */
    private boolean activatableFromHere(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (is(sa.getRestrictions().getZone())) {
                return true;
            }
            if (sa.hasParam("AdditionalActivationZone")) {
                for (final ZoneType zt : ZoneType.listValueOf(sa.getParam("AdditionalActivationZone"))) {
                    if (is(zt)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private final Player player;

    public PlayerZone(final ZoneType zone, final Player inPlayer) {
        super(zone, inPlayer.getGame());
        player = inPlayer;
    }

    @Override
    protected void onChanged() {
        if (getZoneType() == ZoneType.Hand && player.getController().isOrderedZone()) {
            sort();
        }
        player.updateZoneForView(this);
    }

    public final Player getPlayer() {
        return player;
    }

    @Override
    public final String toString() {
        return Lang.getInstance().getPossessedObject(player.toString(), zoneType.toString());
    }

    public Iterable<Card> getCardsPlayerCanActivate(Player who) {
        Iterable<Card> cl = getCards(false);
        boolean checkingForOwner = who == player;

        if (checkingForOwner && (is(ZoneType.Battlefield) || is(ZoneType.Hand))) {
            return cl;
        }

        // Only the top card of the library is normally reachable, but an ability that explicitly
        // names Library as an extra activation zone works at any depth (Unhinged's _____)
        if (is(ZoneType.Library)) {
            final List<Card> reachable = Lists.newArrayList(Iterables.limit(cl, 1));
            for (final Card c : Iterables.skip(cl, 1)) {
                if (activatableFromHere(c)) {
                    reachable.add(c);
                }
            }
            cl = reachable;
        }

        final Predicate<Card> filterPredicate = checkingForOwner ? new OwnCardsActivationFilter() : alienCardsActivationFilter(who);
        return CardLists.filter(cl, filterPredicate);
    }
}
