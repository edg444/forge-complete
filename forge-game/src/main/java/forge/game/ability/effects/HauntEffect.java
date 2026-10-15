package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class HauntEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        if (host.isPermanent() && sa.hasTriggeringObject(AbilityKey.NewCard)) {
            // get new version instead of battlefield lki
            host = (Card) sa.getTriggeringObject(AbilityKey.NewCard);
        }
        final Game game = host.getGame();
        Card card = game.getCardState(host, null);
        if (card == null) {
            return;
        } else if (sa.usesTargeting() && !card.isToken() && host.equalsWithGameTimestamp(card)) {
            // haunt target but only if card is no token and still in grave
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
            final Card moved = game.getAction().exile(card, sa, moveParams);
            final Card haunted = sa.getTargetCard();
            haunted.addHauntedBy(moved);
            zoneMovements.triggerChangesZoneAll(game, sa);
            if (sa.hasParam("ReturnWhenHauntedLeaves")) {
                // Kaya, Ghost Haunter: exiled for as long as that creature remains on the battlefield - she comes back
                // the moment it leaves, if she's still haunting it from exile (her rulings)
                haunted.addLeavesPlayCommand(() -> {
                    final Card current = game.getCardState(moved, null);
                    if (current != null && current.isInZone(ZoneType.Exile) && current.equalsWithGameTimestamp(moved)
                            && haunted.isHauntedBy(current)) {
                        haunted.removeHauntedBy(current);
                        current.setHaunting(null);
                        game.getAction().moveToPlay(current, current.getOwner(), null, AbilityKey.newMap());
                    }
                });
            }
        } else if (!sa.usesTargeting()) {
            // unhaunt - the haunter that left exile is a new object, so the haunting may only be on the one that left
            final Card haunter = card.getHaunting() != null ? card : host.getHaunting() != null ? host : null;
            if (haunter != null) {
                haunter.getHaunting().removeHauntedBy(haunter);
                haunter.setHaunting(null);
            }
        }
    }

}
