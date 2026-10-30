package forge.ai.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiTeams;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.FileSection;

/**
 * AILogic$ RedirectCombat: a damage redirection (Mirror Strike, Sivvi's Valor, Kor Chant, Carom, the en-Kor) is judged
 * against the combat it would change. Combat is modelled as hits - source, victim, damage - and the card's redirection
 * replacement moves damage off its victims onto its destination; the outcome is creatures dying on each side and life
 * lost, lethal counting for everything. Every choice of targets (and of the source, for "a source of your choice") is
 * tried, and the source picked is remembered for when ChooseSource asks.
 */
public final class RedirectCombatAi {

    private enum Scope { ALL, ONCE, AMOUNT }

    private record Redirect(SpellAbility effect, String source, String target, boolean combatOnly, String dest,
            Scope scope, int amount) { }

    private record Hit(Card source, GameEntity victim, int dmg, boolean combat) { }

    // the source the AI meant when it decided, keyed by the host card, for ChooseSource at resolution
    private static final Map<Integer, Card> CHOSEN_SOURCES = new ConcurrentHashMap<>();

    private RedirectCombatAi() { }

    public static Card chosenSource(final Card host) {
        return host == null ? null : CHOSEN_SOURCES.get(host.getId());
    }

    /** The most damage one hit the AI faces right now would deal, as decide sees it (Shining Shoal sizes X on it). */
    public static int biggestHit(final Player ai) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final List<Hit> hits;
        if (!game.getStack().isEmpty()) {
            hits = stackHits(ai, game.getStack().peekAbility());
        } else if (combat != null && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            hits = hits(combat);
        } else {
            hits = List.of();
        }
        return hits.stream().mapToInt(Hit::dmg).max().orElse(0);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final List<Hit> hits;
        if (!game.getStack().isEmpty()) {
            hits = stackHits(ai, game.getStack().peekAbility());
        } else if (combat != null && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            hits = hits(combat);
        } else {
            hits = List.of();
        }
        if (hits.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        final List<Redirect> redirects = redirects(sa);
        if (redirects.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        // shields from this card still waiting in the command zone; a "next time" or "all" one is enough as it is
        final int live = (int) ai.getCardsIn(ZoneType.Command).stream().filter(c -> host.equals(c.getEffectSource())).count();
        final boolean stacking = sa.isAbility() && redirects.stream().allMatch(r -> r.scope() == Scope.AMOUNT);
        if (live > 0 && !stacking) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final List<SpellAbility> tgtSas = new ArrayList<>();
        boolean choosesSource = false;
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                tgtSas.add(sub);
            }
            choosesSource |= sub.getApi() == ApiType.ChooseSource;
        }
        final boolean danger = combat != null && ComputerUtilCombat.lifeInDanger(ai, combat);
        final int base = score(ai, hits, danger);

        final List<List<GameEntity>> options = new ArrayList<>();
        for (final SpellAbility t : tgtSas) {
            t.resetTargets();
            final List<GameEntity> opts = new ArrayList<>(CardUtil.getValidCardsToTarget(t));
            for (final Player p : game.getPlayers()) {
                if (t.canTarget(p)) {
                    opts.add(p);
                }
            }
            options.add(opts);
        }
        final List<Card> sources = new ArrayList<>();
        if (choosesSource) {
            for (final Hit h : hits) {
                if (!sources.contains(h.source())) {
                    sources.add(h.source());
                }
            }
        } else {
            sources.add(null);
        }

        // a repeatable "next 1 damage" goes on until enough has moved, counting the shields already made
        final int done = stacking ? live : 0;
        final int threshold = sa.isSpell() ? 100 : 40;

        int best = Integer.MIN_VALUE;
        List<GameEntity> bestPick = null;
        Card bestSource = null;
        final List<GameEntity> pick = new ArrayList<>();
        final int combos = options.stream().mapToInt(List::size).reduce(1, (a, b) -> a * b);
        for (int i = 0; i < combos; i++) {
            pick.clear();
            int rest = i;
            for (final List<GameEntity> opts : options) {
                pick.add(opts.get(rest % opts.size()));
                rest /= opts.size();
            }
            if (pick.stream().distinct().count() < pick.size()) {
                continue;
            }
            for (final Card src : sources) {
                int value;
                if (stacking) {
                    // worth carrying on only if some number of further activations reaches a worthwhile outcome
                    final int already = done == 0 ? 0 : outcome(ai, sa, redirects, tgtSas, pick, src, hits, danger, done) - base;
                    value = Integer.MIN_VALUE;
                    for (int m = done + 1; m <= done + 4; m++) {
                        final int v = outcome(ai, sa, redirects, tgtSas, pick, src, hits, danger, m) - base;
                        if (v > already) {
                            value = Math.max(value, v);
                        }
                    }
                } else {
                    value = outcome(ai, sa, redirects, tgtSas, pick, src, hits, danger, 1) - base;
                }
                if (value > best) {
                    best = value;
                    bestPick = new ArrayList<>(pick);
                    bestSource = src;
                }
            }
        }
        if (bestPick == null || best < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        for (int i = 0; i < tgtSas.size(); i++) {
            tgtSas.get(i).getTargets().add(bestPick.get(i));
        }
        if (bestSource != null) {
            CHOSEN_SOURCES.put(host.getId(), bestSource);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static List<Redirect> redirects(final SpellAbility sa) {
        final List<Redirect> out = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() != ApiType.Effect) {
                continue;
            }
            for (final String name : sub.getParamOrDefault("ReplacementEffects", "").split(",")) {
                if (name.isEmpty() || !sub.hasSVar(name)) {
                    continue;
                }
                final Map<String, String> re = FileSection.parseToMap(sub.getSVar(name), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                if (!"DamageDone".equals(re.get("Event")) || !re.containsKey("ReplaceWith")) {
                    continue;
                }
                final Map<String, String> with = FileSection.parseToMap(sub.getSVar(re.get("ReplaceWith")), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                // Soltari Guerrillas deals the damage anew instead of moving it
                final String dest = re.containsKey("DamageTarget") ? re.get("DamageTarget")
                        : "DealDamage".equals(with.get("DB")) ? with.get("Defined") : null;
                if (dest == null) {
                    continue;
                }
                Scope scope;
                int amount = 0;
                if (with.containsKey("ReplaceSplitDamage") || "ReplaceSplitDamage".equals(with.get("DB"))) {
                    scope = Scope.AMOUNT;
                    amount = 1;
                    final String var = with.get("VarName");
                    if (var != null && sub.getSVar(var).startsWith("Number$")) {
                        amount = Integer.parseInt(sub.getSVar(var).substring(7));
                    }
                    // Shining Shoal: the shield's size is stored from X as it resolves
                    for (SpellAbility st = sa; var != null && st != null; st = st.getSubAbility()) {
                        if (st.getApi() == ApiType.StoreSVar && var.equals(st.getParam("SVar")) && st.hasParam("Expression")) {
                            amount = AbilityUtils.calculateAmount(sa.getHostCard(), st.getParam("Expression"), sa);
                        }
                    }
                } else {
                    // a "next time" shield exiles its effect once used; without that it lasts the turn
                    scope = exilesItself(sub, with) ? Scope.ONCE : Scope.ALL;
                }
                out.add(new Redirect(sub, re.get("ValidSource"), re.get("ValidTarget"), "True".equals(re.get("IsCombat")),
                        dest, scope, amount));
            }
        }
        return out;
    }

    private static boolean exilesItself(final SpellAbility sub, Map<String, String> step) {
        for (int i = 0; i < 5 && step.containsKey("SubAbility"); i++) {
            step = FileSection.parseToMap(sub.getSVar(step.get("SubAbility")), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
            if ("ChangeZone".equals(step.get("DB")) && "Self".equals(step.get("Defined")) && "Exile".equals(step.get("Destination"))) {
                return true;
            }
        }
        return false;
    }

    // a burn spell or ability the AI is answering: its damage to each target
    private static List<Hit> stackHits(final Player ai, final SpellAbility top) {
        final List<Hit> hits = new ArrayList<>();
        if (top == null || top.getApi() != ApiType.DealDamage || !top.usesTargeting() || !top.getActivatingPlayer().isOpponentOf(ai)
                || !top.hasParam("NumDmg")) {
            return hits;
        }
        final Card src = top.getHostCard();
        final int dmg = AbilityUtils.calculateAmount(src, top.getParam("NumDmg"), top);
        for (final GameEntity e : top.getTargets().getTargetEntities()) {
            final Integer divided = top.isDividedAsYouChoose() ? top.getDividedValue(e) : null;
            hits.add(new Hit(src, e, ComputerUtilCombat.predictDamageTo(e, divided != null ? divided : dmg, src, false), false));
        }
        hits.removeIf(h -> h.dmg() <= 0);
        return hits;
    }

    // combat damage as it would be dealt: unblocked attackers to their player, blocked ones to their blockers in
    // order, and blockers back to what they block
    private static List<Hit> hits(final Combat combat) {
        final List<Hit> hits = new ArrayList<>();
        for (final Card att : combat.getAttackers()) {
            if (combat.isUnblocked(att)) {
                if (combat.getDefenderByAttacker(att) instanceof Player p) {
                    hits.add(new Hit(att, p, ComputerUtilCombat.predictDamageTo(p,
                            ComputerUtilCombat.damageIfUnblocked(att, p, combat, false), att, true), true));
                }
                continue;
            }
            int left = att.getNetCombatDamage();
            final CardCollection blockers = combat.getBlockers(att);
            for (int i = 0; i < blockers.size() && left > 0; i++) {
                final Card b = blockers.get(i);
                final int assign = i == blockers.size() - 1 ? left : Math.min(left, Math.max(1, b.getLethalDamage()));
                left -= assign;
                hits.add(new Hit(att, b, ComputerUtilCombat.predictDamageTo(b, assign, att, true), true));
            }
            for (final Card b : blockers) {
                hits.add(new Hit(b, att, ComputerUtilCombat.predictDamageTo(att, b.getNetCombatDamage(), b, true), true));
            }
        }
        hits.removeIf(h -> h.dmg() <= 0);
        return hits;
    }

    private static int score(final Player ai, final List<Hit> hits, final boolean danger) {
        final Map<GameEntity, Integer> dmg = new HashMap<>();
        final Map<GameEntity, Boolean> deathtouch = new HashMap<>();
        for (final Hit h : hits) {
            dmg.merge(h.victim(), h.dmg(), Integer::sum);
            if (h.source().hasKeyword(Keyword.DEATHTOUCH)) {
                deathtouch.put(h.victim(), true);
            }
        }
        int score = 0;
        for (final Map.Entry<GameEntity, Integer> e : dmg.entrySet()) {
            final int d = e.getValue();
            if (e.getKey() instanceof Card c) {
                if (c.isCreature() && c.canBeDestroyed() && (d >= c.getLethalDamage() || deathtouch.containsKey(c))) {
                    score += AiTeams.sign(ai, c.getController()) * ComputerUtilCard.evaluateCreature(c);
                }
            } else if (e.getKey() instanceof Player p) {
                final boolean dies = d >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife();
                if (AiTeams.sign(ai, p) < 0) {
                    score -= dies ? 1000 : d * (danger && p.equals(ai) ? 30 : 6);
                } else if (p.isOpponentOf(ai)) {
                    score += dies ? 1000 : d * 6;
                }
            }
        }
        return score;
    }

    private static int outcome(final Player ai, final SpellAbility sa, final List<Redirect> redirects,
            final List<SpellAbility> tgtSas, final List<GameEntity> pick, final Card source, final List<Hit> hits,
            final boolean danger, final int times) {
        final Card host = sa.getHostCard();
        int best = Integer.MIN_VALUE;
        // which hit a "next time" or "next N" shield lands on first is open, so take the best of them
        final int firstOptions = Math.max(1, hits.size());
        for (int first = 0; first < firstOptions; first++) {
            final List<Hit> out = new ArrayList<>(hits);
            boolean any = false;
            for (final Redirect r : redirects) {
                final List<GameEntity> remembered = members(r.effect(), "RememberObjects", tgtSas, pick, host);
                final List<GameEntity> imprinted = members(r.effect(), "ImprintCards", tgtSas, pick, host);
                int budget = r.scope() == Scope.AMOUNT ? r.amount() * times : Integer.MAX_VALUE;
                boolean onceUsed = false;
                // only the damage as it stands is redirected, not what this or another shield has already moved
                final int n = out.size();
                for (int k = 0; k < n; k++) {
                    // start from the chosen first hit, then go round
                    final int idx = (k + first) % n;
                    final Hit h = out.get(idx);
                    if (budget <= 0 || h.dmg() <= 0 || onceUsed && r.scope() == Scope.ONCE || !applies(ai, sa, r, h, remembered, imprinted, source)) {
                        continue;
                    }
                    final GameEntity dest = destination(ai, r, h, remembered, imprinted, host);
                    if (dest == null || dest.equals(h.victim())) {
                        continue;
                    }
                    final int moved = Math.min(budget, h.dmg());
                    if (r.scope() == Scope.AMOUNT) {
                        budget -= moved;
                    }
                    onceUsed = true;
                    any = true;
                    out.set(idx, new Hit(h.source(), h.victim(), h.dmg() - moved, h.combat()));
                    out.add(new Hit(h.source(), dest, moved, h.combat()));
                }
            }
            if (any) {
                best = Math.max(best, score(ai, out, danger));
            }
            if (redirects.stream().allMatch(r -> r.scope() == Scope.ALL)) {
                break;
            }
        }
        return best == Integer.MIN_VALUE ? score(ai, hits, danger) : best;
    }

    // what an Effect's RememberObjects / ImprintCards will hold when it resolves
    private static List<GameEntity> members(final SpellAbility effect, final String param, final List<SpellAbility> tgtSas,
            final List<GameEntity> pick, final Card host) {
        final List<GameEntity> out = new ArrayList<>();
        final String what = effect.getParamOrDefault(param, "");
        if (what.equals("Targeted") || what.equals("ThisTargetedCard")) {
            final int i = tgtSas.indexOf(effect);
            if (i >= 0) {
                out.add(pick.get(i));
            }
        } else if (what.equals("ParentTarget")) {
            for (int i = tgtSas.size() - 1; i >= 0; i--) {
                if (tgtSas.get(i) != effect && isAncestor(tgtSas.get(i), effect)) {
                    out.add(pick.get(i));
                    break;
                }
            }
        } else if (what.equals("Enchanted") && host.getEnchantingCard() != null) {
            out.add(host.getEnchantingCard());
        } else if (what.equals("Self")) {
            out.add(host);
        }
        return out;
    }

    private static boolean isAncestor(final SpellAbility a, final SpellAbility b) {
        for (SpellAbility s = a.getSubAbility(); s != null; s = s.getSubAbility()) {
            if (s == b) {
                return true;
            }
        }
        return false;
    }

    private static boolean applies(final Player ai, final SpellAbility sa, final Redirect r, final Hit h,
            final List<GameEntity> remembered, final List<GameEntity> imprinted, final Card source) {
        return (!r.combatOnly() || h.combat())
                && matches(ai, sa, r.source(), h.source(), remembered, imprinted, source)
                && matches(ai, sa, r.target(), h.victim(), remembered, imprinted, source);
    }

    private static boolean matches(final Player ai, final SpellAbility sa, final String valid, final GameEntity entity,
            final List<GameEntity> remembered, final List<GameEntity> imprinted, final Card source) {
        if (valid == null) {
            return true;
        }
        final Card host = sa.getHostCard();
        for (final String token : valid.split(",")) {
            if (token.contains("EffectSource")) {
                if (entity.equals(host)) {
                    return true;
                }
            } else if (token.contains("ChosenCard")) {
                if (source != null && entity.equals(source)) {
                    return true;
                }
            } else if (token.contains("IsRemembered")) {
                if (remembered.contains(entity) && entity.isValid(token.replace("+IsRemembered", "").replace(".IsRemembered", ""), ai, host, sa)) {
                    return true;
                }
            } else if (token.contains("IsImprinted")) {
                if (imprinted.contains(entity) && entity.isValid(token.replace("+IsImprinted", "").replace(".IsImprinted", ""), ai, host, sa)) {
                    return true;
                }
            } else if (entity.isValid(token, ai, host, sa)) {
                return true;
            }
        }
        return false;
    }

    private static GameEntity destination(final Player ai, final Redirect r, final Hit h, final List<GameEntity> remembered,
            final List<GameEntity> imprinted, final Card host) {
        return switch (r.dest()) {
            case "You" -> ai;
            case "Remembered" -> remembered.isEmpty() ? null : remembered.get(0);
            case "Imprinted" -> imprinted.isEmpty() ? null : imprinted.get(0);
            case "ReplacedSourceController" -> h.source().getController();
            case "EffectSource" -> host;
            default -> null;
        };
    }
}
