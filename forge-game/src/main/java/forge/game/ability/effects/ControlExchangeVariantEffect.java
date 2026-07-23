package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;


public class ControlExchangeVariantEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Exchange cards controlled by " + StringUtils.join(getTargetPlayers(sa), ",");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final List<Player> players = getDefinedPlayersOrTargeted(sa);
        if (players.size() != 2) {
            return;
        }
        final Player player1 = players.get(0);
        final Player player2 = players.get(1);
        final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Battlefield"));
        final String type = sa.getParamOrDefault("Type", "Card");
        // get valid lists
        CardCollectionView list1 = AbilityUtils.filterListByType(player1.getCardsIn(zone), type, sa);
        CardCollectionView list2 = AbilityUtils.filterListByType(player2.getCardsIn(zone), type, sa);
        CardCollectionView chosen1;
        CardCollectionView chosen2;
        if (sa.hasParam("All")) {
            // Mirror Mirror: "exchange control of all permanents" - no choice involved, take both
            // lists wholesale instead of prompting for equal-sized subsets.
            chosen1 = list1;
            chosen2 = list2;
        } else {
            int max = Math.min(list1.size(), list2.size());
            // choose the same number of cards
            chosen1 = activator.getController().chooseCardsForEffect(list1, sa, Localizer.getInstance().getMessage("lblChooseCards") + ":" + player1, 0, max, true, null);
            int num = chosen1.size();
            chosen2 = activator.getController().chooseCardsForEffect(list2, sa, Localizer.getInstance().getMessage("lblChooseCards") + ":" + player2, num, num, true, null);
        }
        // check all cards can be controlled by the other player
        for (final Card c : chosen1) {
            if (!c.canBeControlledBy(player2)) {
                return;
            }
        }
        for (final Card c : chosen2) {
            if (!c.canBeControlledBy(player1)) {
                return;
            }
        }
        // set new controller
        final long tStamp = sa.getActivatingPlayer().getGame().getNextTimestamp();
        for (final Card c : chosen1) {
            c.addTempController(player2, tStamp);
        }
        for (final Card c : chosen2) {
            c.addTempController(player1, tStamp);
        }
    }
}
