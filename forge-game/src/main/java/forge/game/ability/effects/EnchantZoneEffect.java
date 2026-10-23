package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Zone of Flame's Aura spell: "Enchant zone". A zone isn't an object, so nothing is targeted; as the spell resolves its
 * controller picks a shared zone (battlefield, command, exile, stack) or one player's graveyard, hand or library, and
 * the Aura enters enchanting it. The AI picks the stack, where every spell comes and goes.
 */
public class EnchantZoneEffect extends SpellAbilityEffect {

    private static final List<ZoneType> SHARED = List.of(ZoneType.Battlefield, ZoneType.Command, ZoneType.Exile, ZoneType.Stack);
    private static final List<ZoneType> OWN = List.of(ZoneType.Graveyard, ZoneType.Hand, ZoneType.Library);

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card aura = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();

        final List<String> labels = Lists.newArrayList();
        for (final ZoneType z : SHARED) {
            labels.add(z.getTranslatedName());
        }
        for (final Player owner : game.getPlayers()) {
            for (final ZoneType z : OWN) {
                labels.add(owner.getName() + "'s " + z.getTranslatedName());
            }
        }
        int pick = SHARED.indexOf(ZoneType.Stack);
        if (!p.getController().isAI()) {
            final String label = p.getController().chooseSomeType("zone", new SpellAbility.EmptySa(ApiType.ChooseType, aura, p),
                    labels, false);
            pick = Math.max(0, labels.indexOf(label));
        }
        final ZoneType zone;
        final Player zoneOwner;
        if (pick < SHARED.size()) {
            zone = SHARED.get(pick);
            zoneOwner = null;
        } else {
            final int rest = pick - SHARED.size();
            zoneOwner = game.getPlayers().get(rest / OWN.size());
            zone = OWN.get(rest % OWN.size());
        }

        final CardZoneTable table = new CardZoneTable();
        aura.setController(p, 0);
        final ZoneType previousZone = aura.getZone().getZoneType();
        aura.setEnchantedZone(zone, zoneOwner);
        final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        final Card c = game.getAction().moveToPlay(aura, p, sa, moveParams);
        c.setEnchantedZone(zone, zoneOwner);
        if (c.getZone().getZoneType() != previousZone) {
            table.put(previousZone, c.getZone().getZoneType(), c);
        }
        table.triggerChangesZoneAll(game, sa);
        game.getGameLog().add(GameLogEntryType.INFORMATION, c + " enchants " + labels.get(pick) + ".");
    }
}
