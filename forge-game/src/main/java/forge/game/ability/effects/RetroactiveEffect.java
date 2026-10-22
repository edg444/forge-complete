package forge.game.ability.effects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * The Game-Changing Host: "Anything that could have changed The Game-Changing Host on the turn it enters applies
 * retroactively." User ruling 2026-10-22: the spells and abilities that resolved earlier this turn apply to it as it
 * enters - every part that affected each creature of a kind it is (PumpAll, DestroyAll, DamageAll and the like) and every
 * targeted part it would have been a legal target for - in the order they resolved, from their own source and
 * controller. Only those parts, never the rest of their effects.
 */
public class RetroactiveEffect extends SpellAbilityEffect {

    // by name: ApiType builds its effects as it initializes, so an EnumSet here would see it half-made
    private static final Set<String> ALL = Set.of("PumpAll", "DestroyAll", "DamageAll", "PutCounterAll", "AnimateAll",
            "TapAll", "UntapAll", "SacrificeAll", "ChangeZoneAll");
    private static final Set<String> TARGETED = Set.of("Pump", "Destroy", "DealDamage", "PutCounter", "Animate", "Tap",
            "Untap", "Debuff", "ChangeZone", "GainControl");
    private static final Set<String> DROP = Set.of("ValidTgts", "TgtPrompt", "TargetMin", "TargetMax", "TargetUnique",
            "ValidCards", "ChangeType", "Defined", "SubAbility", "SpellDescription", "StackDescription", "AILogic",
            "TargetsWithDifferentControllers", "TargetType", "TgtZone", "IsCurse", "Cost");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Everything that could have changed " + sa.getHostCard() + " this turn applies retroactively.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        for (final SpellAbility done : Lists.newArrayList(game.getResolvedThisTurn())) {
            for (SpellAbility part = done; part != null; part = part.getSubAbility()) {
                if (!host.isInPlay()) {
                    return;
                }
                final ApiType api = part.getApi();
                if (api == null || part.getActivatingPlayer() == null || part.getHostCard() == null
                        || part.getHostCard().equals(host)) {
                    continue;
                }
                final boolean all = ALL.contains(api.name());
                if (!all && !TARGETED.contains(api.name())) {
                    continue;
                }
                final String valid = all ? part.getParamOrDefault("ValidCards", part.getParam("ChangeType")) : null;
                final boolean affects;
                if (all) {
                    affects = valid != null && (api != ApiType.ChangeZoneAll || "Battlefield".equals(part.getParam("Origin")))
                            && host.isValid(valid.split(","), part.getActivatingPlayer(), part.getHostCard(), part);
                } else {
                    affects = part.usesTargeting() && (api != ApiType.ChangeZone || "Battlefield".equals(part.getParam("Origin")))
                            && part.canTarget(host);
                }
                if (!affects) {
                    continue;
                }
                replay(part, api, all, host);
            }
        }
    }

    private static void replay(final SpellAbility part, final ApiType api, final boolean all, final Card host) {
        final Map<String, String> params = new LinkedHashMap<>(part.getMapParams());
        params.keySet().removeIf(k -> DROP.contains(k) || k.startsWith("DB") || k.startsWith("SP") || k.startsWith("AB"));
        if (all) {
            params.put(api == ApiType.ChangeZoneAll ? "ChangeType" : "ValidCards", "Card.Self");
        } else {
            params.put("Defined", "Self");
        }
        if ((api == ApiType.DealDamage || api == ApiType.DamageAll) && !params.containsKey("DamageSource")) {
            params.put("DamageSource", "CardUID_" + part.getHostCard().getId());
        }
        final StringBuilder sb = new StringBuilder("DB$ ").append(api.name());
        for (final Map.Entry<String, String> e : params.entrySet()) {
            sb.append(" | ").append(e.getKey()).append("$ ").append(e.getValue());
        }
        final SpellAbility again = AbilityFactory.getAbility(sb.toString(), host, part.getHostCard());
        again.setActivatingPlayer(part.getActivatingPlayer());
        host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, host + " is retroactively affected by "
                + part.getHostCard().getName() + ".");
        AbilityUtils.resolve(again);
    }
}
