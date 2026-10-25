package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.ai.ComputerUtilCard;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

/**
 * Choosing new targets for a spell or ability on the stack (Deflection, Meddle, Redirect, Willbender, Boltbender...):
 * each possible target is scored by how good it is for the AI that the spell hits it - a harmful spell is best on the
 * opponents' most valuable thing, a helpful one on the AI's own.
 */
public final class RetargetAi {

    private static final Set<ApiType> HARMFUL = Set.of(ApiType.DealDamage, ApiType.Destroy, ApiType.Sacrifice,
            ApiType.Tap, ApiType.Counter, ApiType.Discard, ApiType.LoseLife, ApiType.Mill, ApiType.GainControl,
            ApiType.Fight, ApiType.Debuff, ApiType.ChangeZone);

    private RetargetAi() { }

    private static boolean harmful(final SpellAbility s) {
        if (s.isCurse()) {
            return true;
        }
        final ApiType api = s.getApi();
        if (api == ApiType.ChangeZone) {
            // bounce and exile are bad for what they hit; a return from the graveyard is good for its owner
            return s.getParamOrDefault("Origin", "Battlefield").contains("Battlefield");
        }
        if (api == ApiType.Pump) {
            return s.getParamOrDefault("NumAtt", "").startsWith("-") || s.getParamOrDefault("NumDef", "").startsWith("-");
        }
        if (api == ApiType.PutCounter) {
            return s.getParamOrDefault("CounterType", "").startsWith("M1M1");
        }
        return api != null && HARMFUL.contains(api);
    }

    private static int amount(final SpellAbility s, final String param) {
        if (!s.hasParam(param)) {
            return 0;
        }
        return AbilityUtils.calculateAmount(s.getHostCard(), s.getParam(param), s);
    }

    /** How good it is for the AI that s hits o. */
    public static int score(final Player ai, final SpellAbility s, final GameObject o) {
        final boolean bad = harmful(s);
        final Player owner;
        int weight;
        if (o instanceof Card c) {
            owner = c.getController();
            if (c.isCreature()) {
                final int value = ComputerUtilCard.evaluateCreature(c);
                if (s.getApi() == ApiType.DealDamage) {
                    final int dmg = s.isDividedAsYouChoose() ? s.getTotalDividedValue() : amount(s, "NumDmg");
                    weight = dmg >= c.getLethalDamage() && c.canBeDestroyed() ? value : dmg * 4;
                } else if (s.getApi() == ApiType.Destroy && !c.canBeDestroyed()) {
                    weight = 5;
                } else {
                    weight = bad ? value : value / 3;
                }
            } else {
                weight = ComputerUtilCard.evaluatePermanentList(new CardCollection(c));
                if (!bad) {
                    weight /= 3;
                }
            }
        } else if (o instanceof Player p) {
            owner = p;
            final int dmg = s.getApi() == ApiType.DealDamage ? (s.isDividedAsYouChoose() ? s.getTotalDividedValue() : amount(s, "NumDmg"))
                    : s.getApi() == ApiType.LoseLife ? amount(s, "LifeAmount") : -1;
            if (dmg >= 0) {
                weight = dmg >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife() ? 1000 : dmg * 6;
            } else if (s.getApi() == ApiType.Draw) {
                weight = Math.max(1, amount(s, "NumCards")) * 15;
            } else if (s.getApi() == ApiType.GainLife) {
                weight = amount(s, "LifeAmount") * 3;
            } else {
                weight = 30;
            }
        } else if (o instanceof SpellAbility other) {
            owner = other.getActivatingPlayer();
            weight = 50 + other.getHostCard().getCMC() * 30;
        } else {
            return 0;
        }
        if (owner == null) {
            return 0;
        }
        final int sign = owner.equals(ai) || ai.getAllies().contains(owner) ? 1 : owner.isOpponentOf(ai) ? -1 : 0;
        return bad ? -sign * weight : sign * weight;
    }

    private static int total(final Player ai, final SpellAbility s, final Iterable<? extends GameObject> targets) {
        int sum = 0;
        for (final GameObject o : targets) {
            sum += score(ai, s, o);
        }
        return sum;
    }

    /** Every legal target for s that also passes the retargeting card's restriction. */
    public static List<GameObject> candidates(final SpellAbility s, final Predicate<GameObject> filter) {
        final List<GameObject> out = new ArrayList<>(s.getTargetRestrictions().getAllCandidates(s));
        if (s.getTargetRestrictions().getZone().contains(ZoneType.Stack)) {
            for (final SpellAbilityStackInstance si : s.getHostCard().getGame().getStack()) {
                final SpellAbility other = si.getSpellAbility();
                if (other != s && s.canTargetSpellAbility(other)) {
                    out.add(other);
                }
            }
        }
        out.removeIf(o -> filter != null && !filter.test(o) || o instanceof forge.game.GameEntity e && !s.canTarget(e));
        return out;
    }

    /** The best set of as many targets as s has now, or null when none beats what it targets already. */
    public static List<GameObject> best(final Player ai, final SpellAbility s, final Predicate<GameObject> filter) {
        if (!s.usesTargeting()) {
            return null;
        }
        final TargetChoices old = s.getTargets();
        final int n = Math.max(1, Math.max(old.size(), s.getMinTargets()));
        final List<GameObject> cands = candidates(s, filter);
        cands.sort(Comparator.comparingInt((GameObject o) -> score(ai, s, o)).reversed());
        if (cands.size() < n) {
            return null;
        }
        final List<GameObject> pick = new ArrayList<>(cands.subList(0, n));
        return total(ai, s, pick) > total(ai, s, old) ? pick : null;
    }

    /** What changing the targets of the spell on the stack would gain the AI. */
    public static int gain(final Player ai, final SpellAbility spell, final Predicate<GameObject> filter) {
        int gain = 0;
        for (SpellAbility s = spell; s != null; s = s.getSubAbility()) {
            final List<GameObject> pick = best(ai, s, filter);
            if (pick != null) {
                gain += total(ai, s, pick) - total(ai, s, s.getTargets());
            }
        }
        return gain;
    }

    /** The best of the new targets, as a copy of this spell would have them (Wild Ricochet). */
    public static int copyValue(final Player ai, final SpellAbility spell, final Predicate<GameObject> filter) {
        int v = 0;
        for (SpellAbility sub = spell; sub != null; sub = sub.getSubAbility()) {
            final SpellAbility s = sub;
            if (s.usesTargeting()) {
                v += candidates(s, filter).stream().mapToInt(o -> score(ai, s, o)).max().orElse(0);
            }
        }
        return Math.max(0, v);
    }
}
