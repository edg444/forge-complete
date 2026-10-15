package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Zyym, Mesmeric Lord: the target player orders their hand, then reveals it one card at a time until the
 * activator says stop, and discards the most recently revealed card. Each reveal waits for that answer (its ruling).
 */
public class RevealUntilStopEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final Map<Player, CardCollectionView> discards = Maps.newHashMap();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            final CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
            if (hand.isEmpty()) {
                continue;
            }
            final CardCollectionView order = p.getController().orderMoveToZoneList(hand, ZoneType.None, sa);
            final CardCollection revealed = new CardCollection();
            for (final Card c : order) {
                revealed.add(c);
                game.getAction().reveal(new CardCollection(c), p, true);
                if (revealed.size() == order.size()) {
                    break;
                }
                final Map<String, Object> params = Maps.newHashMap();
                params.put("Revealed", revealed);
                params.put("HandSize", order.size());
                if (you.getController().confirmAction(sa, null,
                        "Stop here? " + p + " discards " + c.getName() + ".", c, params)) {
                    break;
                }
            }
            discards.put(p, new CardCollection(revealed.getLast()));
        }

        final Map<AbilityKey, Object> params = AbilityKey.newMap();
        final CardZoneTable table = AbilityKey.addCardZoneTableParams(params, sa);
        discard(sa, true, discards, params);
        table.triggerChangesZoneAll(game, sa);
    }
}
