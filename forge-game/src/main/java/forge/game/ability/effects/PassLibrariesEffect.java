package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Wheel of Not Ideal: each player passes their library, in its order, to the player on their left - the next player
 * in turn order. Nobody's cards change owner; they're just in someone else's library now, and leave it for their
 * owner's graveyard or hand like any other card.
 */
public class PassLibrariesEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Each player passes their library to the player to their left.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final List<Player> players = Lists.newArrayList(game.getPlayersInTurnOrder());
        if (players.size() < 2) {
            return;
        }
        final Map<Player, List<Card>> libraries = Maps.newHashMap();
        for (final Player p : players) {
            libraries.put(p, Lists.newArrayList(p.getCardsIn(ZoneType.Library)));
        }
        for (int i = 0; i < players.size(); i++) {
            final Player to = players.get((i + 1) % players.size());
            to.getZone(ZoneType.Library).setCards(libraries.get(players.get(i)));
        }
        game.getGameLog().add(forge.game.GameLogEntryType.STACK_RESOLVE, "Each player passed their library to the left.");
    }
}
