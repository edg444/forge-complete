package forge.game.trigger;

import java.util.List;
import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.Localizer;

public class TriggerChangesZoneAll extends Trigger {

    public TriggerChangesZoneAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);

        // leaves the GY trigger look back in time
        if (getActiveZone().contains(ZoneType.Battlefield) && "Graveyard".equals(getParam("Origin"))
                && !table.getLastStateBattlefield().contains(getHostCard())) {
            return false;
        }

        if (hasParam("FirstTime")) {
            // currently only for Crawling Sensation
            List<Card> entered = CardUtil.getThisTurnEntered(ZoneType.smartValueOf(getParam("Destination")), null, getParam("ValidCards"), getHostCard(), this, getHostCard().getController());
            entered.removeAll(filterCards(table));
            if (!entered.isEmpty()) {
                return false;
            }
        }

        if (!matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause))) {
            return false;
        }

        if (hasParam("ValidAmount")) {
            int right = AbilityUtils.calculateAmount(hostCard, getParam("ValidAmount").substring(2), this);
            if (!Expressions.compare(this.filterCards(table).size(), getParam("ValidAmount").substring(0, 2), right)) {
                return false;
            }
        }

        return !filterCards(table).isEmpty();
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final CardZoneTable table = (CardZoneTable) runParams.get(AbilityKey.Cards);

        CardCollection allCards = this.filterCards(table);

        sa.setTriggeringObject(AbilityKey.Cards, allCards);
        sa.setTriggeringObject(AbilityKey.Amount, allCards.size());
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Cause);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.Amount));
        return sb.toString();
    }

    private CardCollection filterCards(CardZoneTable table) {
        List<ZoneType> destination = null;
        List<ZoneType> origin = null;

        if (hasParam("Destination") && !getParam("Destination").equals("Any")) {
            destination = ZoneType.listValueOf(getParam("Destination"));
        }

        if (hasParam("Origin") && !getParam("Origin").equals("Any")) {
            origin = ZoneType.listValueOf(getParam("Origin"));
        }

        final String valid = this.getParam("ValidCards");

        // Zone of Flame: cards entering or leaving the zone its host enchants (that player's, for one of theirs)
        if (hasParam("EnchantedZone")) {
            final ZoneType zone = getHostCard().getEnchantedZone();
            final forge.game.player.Player owner = getHostCard().getEnchantedZoneOwner();
            final CardCollection moved = new CardCollection();
            if (zone == null) {
                return moved;
            }
            final List<ZoneType> only = List.of(zone);
            for (final Card c : table.filterCards(only, null, valid, getHostCard(), this)) {
                if (owner == null || owner.equals(c.getOwner())) {
                    moved.add(c);
                }
            }
            for (final Card c : table.filterCards(null, only, valid, getHostCard(), this)) {
                if ((owner == null || owner.equals(c.getOwner())) && !moved.contains(c)) {
                    moved.add(c);
                }
            }
            return moved;
        }

        return table.filterCards(origin, destination, valid, getHostCard(), this);
    }
}
