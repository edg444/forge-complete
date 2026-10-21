package forge.game.staticability;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.token.TokenInfo;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

/**
 * Icing Manipulator: each +1/+1 counter on a creature (ValidCard$) is also a token (TokenScript$, a Food). Each such
 * counter has a stand-in token linked to the creature it's on - the counter stays on that creature while it's a
 * permanent too (rulings). The stand-ins neither enter nor leave the battlefield: they start and stop being permanents
 * as the counters come and go, the way Animate Library's library does. One that leaves on its own (sacrificed for its
 * life, exiled, bounced) takes its counter with it. Kept in step whenever state-based actions are checked.
 */
public class StaticAbilityCountersAreTokens {

    /** Brings the stand-ins in step with the counters; true if anything changed. */
    public static boolean sync(final Game game) {
        // each creature whose counters are tokens, and the static ability that makes them so
        final Map<Card, StaticAbility> wanted = Maps.newLinkedHashMap();
        for (final Card source : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : source.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CountersAreTokens)) {
                    continue;
                }
                for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                    if (c.getCounterTokenHost() == null && stAb.matchesValidParam("ValidCard", c)) {
                        wanted.putIfAbsent(c, stAb);
                    }
                }
            }
        }
        final Map<Card, List<Card>> standIns = Maps.newLinkedHashMap();
        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getCounterTokenHost() != null) {
                standIns.computeIfAbsent(c.getCounterTokenHost(), k -> Lists.newArrayList()).add(c);
            }
        }
        final Set<Card> hosts = Sets.newLinkedHashSet(wanted.keySet());
        hosts.addAll(standIns.keySet());
        boolean changed = false;
        for (final Card host : hosts) {
            final List<Card> have = standIns.getOrDefault(host, Lists.newArrayList());
            final boolean onIt = host.isInPlay() && game.getCardState(host) == host;
            if (onIt && have.size() < host.getCounterTokens()) {
                // the ones that went took their counters with them
                host.subtractCounter(CounterEnumType.P1P1, host.getCounterTokens() - have.size(), host.getController());
                changed = true;
            }
            final int target = onIt && wanted.containsKey(host) ? host.getCounters(CounterEnumType.P1P1) : 0;
            while (have.size() > target) {
                game.getAction().ceaseToExist(have.remove(have.size() - 1), true);
                changed = true;
            }
            while (have.size() < target) {
                have.add(createStandIn(host, wanted.get(host)));
                changed = true;
            }
            host.setCounterTokens(target);
        }
        return changed;
    }

    private static Card createStandIn(final Card host, final StaticAbility stAb) {
        final Game game = host.getGame();
        final Card token = TokenInfo.getProtoType(stAb.getParam("TokenScript"), stAb.getHostCard().getFirstSpellAbility(),
                host.getController(), false);
        token.setOwner(host.getController());
        token.setController(host.getController(), 0);
        token.setCounterTokenHost(host);
        token.setGameTimestamp(game.getNextTimestamp());
        token.setLayerTimestamp(game.getNextTimestamp());
        final Zone battlefield = host.getController().getZone(ZoneType.Battlefield);
        battlefield.add(token);
        token.setZone(battlefield);
        return token;
    }
}
