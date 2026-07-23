package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

// Gerrymandering: "Exile all lands. Give each player a number of those cards chosen at random
// equal to the number of those cards the player controlled." Each player's original count has to
// be captured before anything moves (once every matching permanent is a single shared exiled pool,
// there's no way to recover "how many did this player have" from the cards themselves), then the
// whole pool is shuffled and dealt back out by those captured counts - the DSL has no way to track
// a per-player integer across an arbitrary number of players, so this needed real Java.
public class GerrymanderEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }
        return sa.getHostCard().getName() + " - redistribute matching permanents randomly.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String valid = sa.getParamOrDefault("ValidCards", "Land");

        final CardCollectionView allMatching = CardLists.getValidCards(
                game.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), host, sa);

        final Map<Player, Integer> entitlements = Maps.newHashMap();
        for (Player p : game.getPlayers()) {
            entitlements.put(p, CardLists.filterControlledBy(allMatching, p).size());
        }

        final CardZoneTable triggerList = CardZoneTable.getSimultaneousInstance(sa);

        final CardCollection exiled = new CardCollection();
        for (Card c : allMatching) {
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(moveParams, triggerList);
            Card movedCard = game.getAction().moveTo(ZoneType.Exile, c, 0, sa, moveParams);
            exiled.add(movedCard);
        }

        CardLists.shuffle(exiled);

        int idx = 0;
        for (Player p : game.getPlayers()) {
            int n = entitlements.getOrDefault(p, 0);
            for (int i = 0; i < n && idx < exiled.size(); i++, idx++) {
                Card c = exiled.get(idx);
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                AbilityKey.addCardZoneTableParams(moveParams, triggerList);
                moveParams.put(AbilityKey.SimultaneousETB, exiled);
                c.setController(p, game.getNextTimestamp());
                game.getAction().moveToPlay(c, p, sa, moveParams);
            }
        }

        triggerList.triggerChangesZoneAll(game, sa);
    }
}
