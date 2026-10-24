package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sacrifice for a payoff that depends on what was sacrificed.
 * <ul>
 * <li>AILogic$ SacrificeFor (Carrion, Life's Legacy, Call for Blood, Ghoulcaller Gisa, Sanguine Praetor...): X is the
 * sacrificed creature's power, toughness or mana value, so each creature it could sacrifice is tried - the payoff with
 * its X, less what the creature itself is worth (nothing when it's about to die anyway). The cost then sacrifices that
 * creature.</li>
 * <li>AILogic$ SacrificeAny (Last-Ditch Effort, Landslide): "sacrifice any number" for damage - when that much is
 * lethal, the least of them that is.</li>
 * </ul>
 */
public final class SacrificePayoffAi {

    private static final Map<Integer, CardCollection> PLANNED = new ConcurrentHashMap<>();
    private static final Pattern TOKEN_PT = Pattern.compile("_(\\d+)_(\\d+)_");

    private SacrificePayoffAi() { }

    public static boolean handles(final String logic) {
        return "SacrificeFor".equals(logic) || "SacrificeAny".equals(logic);
    }

    /** What the AI decided to sacrifice for this card, if anything. */
    public static CardCollection planned(final SpellAbility sa) {
        if (sa == null || sa.getHostCard() == null || !handles(sa.getRootAbility().getParam("AILogic"))) {
            return null;
        }
        return PLANNED.get(sa.getHostCard().getId());
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return "SacrificeAny".equals(sa.getParam("AILogic")) ? sacrificeAny(ai, sa) : sacrificeFor(ai, sa);
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
    }

    // "Sacrificed$CardPower/Plus.1" and the like, for the card that would be sacrificed
    private static int x(final SpellAbility s, final String param, final List<Card> sac) {
        String v = s.getParamOrDefault(param, "");
        if (v.isEmpty()) {
            return 0;
        }
        v = v.startsWith("-") ? v.substring(1) : v;
        final String expr = s.hasSVar(v) ? s.getSVar(v) : v;
        if (!expr.startsWith("Sacrificed$") && !expr.startsWith("RememberedLKI$")) {
            return AbilityUtils.calculateAmount(s.getHostCard(), v, s);
        }
        final String[] parts = expr.substring(expr.indexOf('$') + 1).split("/");
        int n = 0;
        for (final Card c : sac) {
            n += switch (parts[0]) {
                case "CardPower" -> c.getNetPower();
                case "CardToughness" -> c.getNetToughness();
                case "CardManaCost" -> c.getCMC();
                default -> parts[0].startsWith("CardCounters.") ? c.getCounters(CounterType.getType(parts[0].substring(13))) : 0;
            };
        }
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("Plus.")) {
                n += Integer.parseInt(parts[i].substring(5));
            } else if (parts[i].equals("Twice")) {
                n *= 2;
            }
        }
        return n;
    }

    private static boolean conditionMet(final Player ai, final SpellAbility s, final List<Card> sac) {
        if (!"Sacrificed".equals(s.getParam("ConditionDefined")) || !s.hasParam("ConditionPresent")) {
            return true;
        }
        return sac.stream().anyMatch(c -> c.isValid(s.getParam("ConditionPresent").split(","), ai, s.getHostCard(), s));
    }

    private static int tokenValue(final String script) {
        final Matcher m = TOKEN_PT.matcher(script == null ? "" : script);
        if (!m.find()) {
            return 40;
        }
        return 20 + 20 * Integer.parseInt(m.group(1)) + 10 * Integer.parseInt(m.group(2));
    }

    private static int side(final Player ai, final Card c) {
        return c.getController().equals(ai) ? -1 : c.getController().isOpponentOf(ai) ? 1 : 0;
    }

    // best target for damage: a kill, or the face; lethal outweighs everything
    private static GameEntity damageTarget(final Player ai, final SpellAbility s, final int dmg, final int[] value) {
        GameEntity best = null;
        int bestV = Integer.MIN_VALUE;
        s.resetTargets();
        final List<GameEntity> opts = new ArrayList<>(CardUtil.getValidCardsToTarget(s));
        for (final Player p : ai.getGame().getPlayers()) {
            if (s.canTarget(p)) {
                opts.add(p);
            }
        }
        for (final GameEntity e : opts) {
            int v = Integer.MIN_VALUE;
            if (e instanceof Player p && p.isOpponentOf(ai)) {
                v = dmg >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife() ? 1000 : dmg * 6;
            } else if (e instanceof Card c && c.getController().isOpponentOf(ai)) {
                v = c.isCreature() && dmg >= c.getLethalDamage() && c.canBeDestroyed() ? ComputerUtilCard.evaluateCreature(c) : dmg * 2;
            }
            if (v > bestV) {
                bestV = v;
                best = e;
            }
        }
        value[0] = best == null ? 0 : bestV;
        return best;
    }

    /** What the effects are worth for the AI with these cards sacrificed; targets are set as a side effect. */
    private static int payoff(final Player ai, final SpellAbility sa, final List<Card> sac) {
        int total = 0;
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (!conditionMet(ai, s, sac)) {
                continue;
            }
            final ApiType api = s.getApi();
            if (api == null) {
                continue;
            }
            switch (api) {
                case Pump -> {
                    // Call for Blood: -X/-X on their best creature it kills
                    final int n = x(s, "NumDef", sac);
                    s.resetTargets();
                    final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(s),
                            c -> c.getController().isOpponentOf(ai) && c.getNetToughness() <= n && !sac.contains(c)));
                    if (t != null) {
                        s.getTargets().add(t);
                        total += ComputerUtilCard.evaluateCreature(t);
                    }
                }
                case PumpAll -> {
                    final int n = x(s, "NumDef", sac);
                    for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield),
                            s.getParamOrDefault("ValidCards", "Creature"), ai, s.getHostCard(), s)) {
                        if (!sac.contains(c) && c.getNetToughness() <= n) {
                            total += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                }
                case DestroyAll -> {
                    // Sanguine Praetor: each creature with the sacrificed creature's mana value
                    final int n = sac.isEmpty() ? -1 : sac.get(0).getCMC();
                    for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                        if (c.isCreature() && !sac.contains(c) && c.getCMC() == n && c.canBeDestroyed()) {
                            total += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                }
                case Token -> total += x(s, "TokenAmount", sac) * tokenValue(s.getParam("TokenScript"));
                case Draw -> total += x(s, "NumCards", sac) * 50;
                case Discard -> {
                    if (s.usesTargeting()) {
                        s.resetTargets();
                        final Player opp = ai.getWeakestOpponent();
                        if (opp == null || !s.canTarget(opp)) {
                            continue;
                        }
                        s.getTargets().add(opp);
                        final CardCollectionView hand = opp.getCardsIn(ZoneType.Hand);
                        if (s.hasParam("DiscardValid")) {
                            // Mind Extraction: every card sharing a color with the sacrificed creature
                            total += (int) hand.stream().filter(c -> sac.stream().anyMatch(c::sharesColorWith)).count() * 50;
                        } else {
                            total += Math.min(hand.size(), x(s, "NumCards", sac)) * 50;
                        }
                    } else {
                        // Greater Good: its own discard after the draw
                        total -= x(s, "NumCards", sac) * 40;
                    }
                }
                case GainLife -> total += x(s, "LifeAmount", sac) * (ComputerUtil.aiLifeInDanger(ai, false, 0) ? 15 : 3);
                case PutCounter -> total += x(s, "CounterNum", sac) * 35;
                case DealDamage -> {
                    final int dmg = x(s, "NumDmg", sac);
                    final int[] v = new int[1];
                    final GameEntity t = damageTarget(ai, s, dmg, v);
                    if (t != null) {
                        s.getTargets().add(t);
                        total += v[0];
                    }
                }
                default -> { }
            }
        }
        return total;
    }

    private static CostSacrifice sacCost(final SpellAbility sa) {
        if (sa.getPayCosts() == null) {
            return null;
        }
        for (final CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof CostSacrifice sac) {
                return sac;
            }
        }
        return null;
    }

    private static AiAbilityDecision sacrificeFor(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final CostSacrifice cost = sacCost(sa);
        if (cost == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final List<GameObject> doomed = ai.getGame().getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean quiet = ai.getGame().getStack().isEmpty() && (ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2)
                || ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai));
        final List<List<Card>> choices = new ArrayList<>();
        if (cost.payCostFromSource()) {
            choices.add(List.of(host));
        } else {
            final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getTypeFor(ai).split(";"), ai, host, sa);
            if ("All".equals(cost.getAmount())) {
                choices.add(valid);
            } else if (cost.getAbilityAmount(sa) == 1) {
                valid.forEach(c -> choices.add(List.of(c)));
            }
        }
        int best = Integer.MIN_VALUE;
        List<Card> bestSac = null;
        for (final List<Card> sac : choices) {
            if (sac.isEmpty()) {
                continue;
            }
            final boolean free = doomed.containsAll(sac);
            // a creature about to die anyway is given up for nothing; otherwise wait for a quiet moment
            if (!free && !quiet) {
                continue;
            }
            final int lost = free ? 0 : sac.stream().mapToInt(SacrificePayoffAi::worth).sum();
            final int v = payoff(ai, sa, sac) - lost;
            if (v > best) {
                best = v;
                bestSac = sac;
            }
        }
        final int threshold = sa.isSpell() ? 40 : 25;
        if (bestSac == null || best < threshold) {
            return new AiAbilityDecision(0, quiet ? AiPlayDecision.CantPlayAi : AiPlayDecision.AnotherTime);
        }
        // the targets go with the chosen sacrifice
        payoff(ai, sa, bestSac);
        PLANNED.put(host.getId(), new CardCollection(bestSac));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision sacrificeAny(final Player ai, final SpellAbility sa) {
        if (sa.getApi() != ApiType.Sacrifice || sa.getSubAbility() == null || sa.getSubAbility().getApi() != ApiType.DealDamage) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        final SpellAbility dmg = sa.getSubAbility();
        final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield),
                sa.getParamOrDefault("SacValid", "Permanent").split(","), ai, host, sa);
        valid.remove(host);
        valid.sort(Comparator.comparingInt(SacrificePayoffAi::worth));
        dmg.resetTargets();
        for (final Player opp : ai.getOpponents()) {
            if (!dmg.canTarget(opp) || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
                continue;
            }
            final int need = forge.ai.ComputerUtilCombat.predictDamageTo(opp, opp.getLife(), host, false) >= opp.getLife()
                    ? opp.getLife() : Integer.MAX_VALUE;
            if (need <= valid.size()) {
                dmg.getTargets().add(opp);
                PLANNED.put(host.getId(), new CardCollection(valid.subList(0, need)));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
