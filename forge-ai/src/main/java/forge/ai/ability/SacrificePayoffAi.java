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
 * lethal, the least of them that is. For a card or life each (Reprocess, Plunge into Darkness) or Kylox's free spells,
 * its junk.</li>
 * </ul>
 */
public final class SacrificePayoffAi {

    private static final Map<Integer, CardCollection> PLANNED = new ConcurrentHashMap<>();
    private static final Pattern TOKEN_PT = Pattern.compile("_(\\d+)_(\\d+)_");

    private SacrificePayoffAi() { }

    public static boolean handles(final String logic) {
        return "SacrificeFor".equals(logic) || "SacrificeAny".equals(logic) || "RevealFor".equals(logic) || "ExileForX".equals(logic)
                || "ExileFor".equals(logic) || "RevealX".equals(logic) || "SacXForX".equals(logic)
                || "SacForLethal".equals(logic) || "SacPumpLethal".equals(logic) || "SacXJunk".equals(logic)
                || "SacRitual".equals(logic) || "TapFor".equals(logic) || "ReshapeX".equals(logic) || "DiscardMv".equals(logic);
    }

    // logics decided elsewhere that still leave a sacrifice plan for the cost (Scapegoat)
    private static boolean plans(final String logic) {
        return handles(logic) || "SaveBounce".equals(logic) || "TwoKill".equals(logic);
    }

    /** Another logic's choice of what the cost sacrifices (Scapegoat: the creature it doesn't save). */
    public static void plan(final Card host, final Card sacrificed) {
        PLANNED.put(host.getId(), new CardCollection(sacrificed));
    }

    /** The logic this ability or its root runs on (Plunge into Darkness: on the Charm's mode, not the Charm). */
    public static String logicOf(final SpellAbility sa) {
        if (sa == null) {
            return null;
        }
        if (plans(sa.getParam("AILogic"))) {
            return sa.getParam("AILogic");
        }
        final String root = sa.getRootAbility().getParam("AILogic");
        return plans(root) ? root : null;
    }

    /** What the AI decided to sacrifice for this card, if anything. */
    public static CardCollection planned(final SpellAbility sa) {
        if (sa == null || sa.getHostCard() == null || logicOf(sa) == null) {
            return null;
        }
        return PLANNED.get(sa.getHostCard().getId());
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "SacrificeAny" -> sacrificeAny(ai, sa);
            case "ExileForX" -> exileForX(ai, sa);
            case "ExileFor" -> exileFor(ai, sa);
            case "RevealX" -> revealX(ai, sa);
            case "SacXForX" -> sacXForX(ai, sa);
            case "SacForLethal" -> sacForLethal(ai, sa);
            case "SacPumpLethal" -> sacPumpLethal(ai, sa);
            case "SacXJunk" -> sacXJunk(ai, sa);
            case "SacRitual" -> sacRitual(ai, sa);
            case "TapFor" -> tapFor(ai, sa);
            case "DiscardMv" -> discardMv(ai, sa);
            case "ReshapeX" -> reshape(ai, sa);
            default -> sacrificeFor(ai, sa);
        };
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
        if (!expr.startsWith("Sacrificed$") && !expr.startsWith("RememberedLKI$") && !expr.startsWith("Revealed$")
                && !expr.startsWith("Exiled$")) {
            return AbilityUtils.calculateAmount(s.getHostCard(), v, s);
        }
        final String[] parts = expr.substring(expr.indexOf('$') + 1).split("/");
        int n = 0;
        for (final Card c : sac) {
            n += switch (parts[0]) {
                case "CardPower" -> c.getNetPower();
                case "CardToughness" -> c.getNetToughness();
                case "CardManaCost" -> c.getCMC();
                case "ChromaSource.Red" -> c.getManaCost().getShardCount(forge.card.mana.ManaCostShard.RED)
                        + c.getManaCost().getShardCount(forge.card.mana.ManaCostShard.RG) + c.getManaCost().getShardCount(forge.card.mana.ManaCostShard.BR)
                        + c.getManaCost().getShardCount(forge.card.mana.ManaCostShard.RW) + c.getManaCost().getShardCount(forge.card.mana.ManaCostShard.UR);
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
                case DamageAll -> {
                    // Disaster Radius: the revealed card's mana value to each creature it hits
                    final int n = x(s, "NumDmg", sac);
                    for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield),
                            s.getParamOrDefault("ValidCards", "Creature"), ai, s.getHostCard(), s)) {
                        if (!sac.contains(c) && n >= c.getLethalDamage() && c.canBeDestroyed()) {
                            total += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                }
                case DestroyAll -> {
                    // Sanguine Praetor: each creature with the sacrificed creature's mana value; Endemic Plague: each
                    // sharing a creature type with it
                    final boolean byType = s.getParamOrDefault("ValidCards", "").contains("sharesCreatureTypeWith Sacrificed");
                    final int n = sac.isEmpty() ? -1 : sac.get(0).getCMC();
                    for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                        final boolean hit = byType ? !sac.isEmpty() && c.sharesCreatureTypeWith(sac.get(0)) : c.getCMC() == n;
                        if (c.isCreature() && !sac.contains(c) && hit && c.canBeDestroyed()) {
                            total += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                }
                case Destroy -> {
                    // Death Bomb: their best creature it may target
                    if (s.usesTargeting()) {
                        s.resetTargets();
                        final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(s),
                                c -> c.getController().isOpponentOf(ai) && c.canBeDestroyed() && !sac.contains(c)));
                        if (t != null) {
                            s.getTargets().add(t);
                            total += ComputerUtilCard.evaluateCreature(t);
                        }
                    }
                }
                case ChangeZone -> {
                    // Diabolic Intent: a tutor
                    if (s.getParamOrDefault("Origin", "").contains("Library") && "Hand".equals(s.getParam("Destination"))) {
                        total += 110;
                    }
                    // Titan's Presence: exile their creature if its power is at most the revealed card's
                    if (s.usesTargeting() && "LEY".equals(s.getParam("ConditionSVarCompare")) && s.getSVar("Y").startsWith("Revealed$")) {
                        final int cap = sac.isEmpty() ? -1 : sac.get(0).getNetPower();
                        s.resetTargets();
                        final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(s),
                                c -> c.getController().isOpponentOf(ai) && c.getNetPower() <= cap));
                        if (t != null) {
                            s.getTargets().add(t);
                            total += ComputerUtilCard.evaluateCreature(t);
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
                case PutCounter -> {
                    // Forge Armor: on its best creature
                    if (s.usesTargeting()) {
                        s.resetTargets();
                        final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(s),
                                c -> c.getController().equals(ai) && !sac.contains(c)));
                        if (t == null) {
                            continue;
                        }
                        s.getTargets().add(t);
                    }
                    total += x(s, "CounterNum", sac) * 35;
                }
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

    private static <T extends CostPart> T costPart(final SpellAbility sa, final Class<T> kind) {
        if (sa.getPayCosts() != null) {
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (kind.isInstance(part)) {
                    return kind.cast(part);
                }
            }
        }
        return null;
    }

    // Charge of the Forever-Beast, Disaster Radius, Induce Despair: revealing costs nothing, so it's the creature card
    // in hand whose X does the most
    private static AiAbilityDecision revealFor(final Player ai, final SpellAbility sa) {
        final forge.game.cost.CostReveal reveal = costPart(sa, forge.game.cost.CostReveal.class);
        if (reveal == null || !ai.getGame().getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        int best = Integer.MIN_VALUE;
        Card bestCard = null;
        for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Hand), reveal.getType().split(";"), ai, host, sa)) {
            if (c.equals(host)) {
                continue;
            }
            final int v = payoff(ai, sa, List.of(c));
            if (v > best) {
                best = v;
                bestCard = c;
            }
        }
        if (bestCard == null || best < (sa.isSpell() ? 60 : 30)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        payoff(ai, sa, List.of(bestCard));
        PLANNED.put(host.getId(), new CardCollection(bestCard));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static CostSacrifice sacXCost(final SpellAbility sa) {
        if (sa.getPayCosts() != null) {
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostSacrifice sac && !sac.payCostFromSource()) {
                    return sac;
                }
            }
        }
        return null;
    }

    // Sword of the Ages: its creatures' total power to the face - just enough of them for lethal
    private static AiAbilityDecision sacForLethal(final Player ai, final SpellAbility sa) {
        final CostSacrifice cost = sacXCost(sa);
        final Player opp = ai.getWeakestOpponent();
        if (cost == null || opp == null || !sa.usesTargeting() || !sa.canTarget(opp) || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final List<Card> valid = new ArrayList<>(CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), ai, sa.getHostCard(), sa));
        valid.remove(sa.getHostCard());
        valid.sort(Comparator.comparingInt(Card::getNetPower).reversed());
        final CardCollection plan = new CardCollection();
        int power = 0;
        for (final Card c : valid) {
            if (power >= opp.getLife()) {
                break;
            }
            plan.add(c);
            power += c.getNetPower();
        }
        if (power < opp.getLife()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(plan.size());
        sa.resetTargets();
        sa.getTargets().add(opp);
        PLANNED.put(sa.getHostCard().getId(), plan);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Vicious Betrayal, Devouring Rage: creatures sacrificed to pump an unblocked attacker - just enough for lethal
    private static AiAbilityDecision sacPumpLethal(final Player ai, final SpellAbility sa) {
        final forge.game.combat.Combat combat = ai.getGame().getCombat();
        final CostSacrifice cost = sacXCost(sa);
        if (cost == null || combat == null || !ai.equals(combat.getAttackingPlayer())
                || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        final List<Card> fodder = new ArrayList<>(CardLists.filter(CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield),
                cost.getType().split(";"), ai, sa.getHostCard(), sa), c -> !combat.isAttacking(c) || !combat.isUnblocked(c)));
        fodder.sort(Comparator.comparingInt(SacrificePayoffAi::worth));
        for (final Card att : combat.getAttackers()) {
            final Player def = combat.getDefenderPlayerByAttacker(att);
            if (def == null || !combat.isUnblocked(att) || !sa.canTarget(att) || !def.canLoseLife() || def.cantLoseForZeroOrLessLife()) {
                continue;
            }
            final int coming = forge.ai.ComputerUtilCombat.sumDamageIfUnblocked(CardLists.filter(combat.getAttackers(), combat::isUnblocked), def);
            final CardCollection plan = new CardCollection();
            for (final Card f : fodder) {
                if (f.equals(att)) {
                    continue;
                }
                plan.add(f);
                sa.setXManaCostPaid(plan.size());
                if (coming + sacAmount(sa, sa.getParamOrDefault("NumAtt", "0").replace("+", ""), plan.size()) >= def.getLife()) {
                    sa.resetTargets();
                    sa.getTargets().add(att);
                    PLANNED.put(sa.getHostCard().getId(), plan);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    // "SVar$Y/Times.2" over "Sacrificed$Amount/Plus.1" and the like, for n cards sacrificed
    private static int sacAmount(final SpellAbility sa, final String name, final int n) {
        final String expr = sa.getSVar(name);
        if (expr.isEmpty()) {
            return 0;
        }
        final String[] parts = expr.split("/");
        int v;
        if (parts[0].startsWith("SVar$")) {
            v = sacAmount(sa, parts[0].substring(5), n);
        } else if (parts[0].equals("Sacrificed$Amount")) {
            v = n;
        } else {
            return 0;
        }
        for (int i = 1; i < parts.length; i++) {
            final String op = parts[i];
            if (op.startsWith("Times.")) {
                v *= Integer.parseInt(op.substring(6));
            } else if (op.startsWith("Plus.")) {
                v += Integer.parseInt(op.substring(5));
            } else if (op.equals("Twice")) {
                v *= 2;
            } else if (op.equals("Thrice")) {
                v *= 3;
            }
        }
        return v;
    }

    // Copper-Leaf Angel, Nahiri's Lithoforming: X is the lands it can spare
    private static AiAbilityDecision sacXJunk(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final CostSacrifice cost = sacXCost(sa);
        final String type = cost != null ? cost.getType() : sa.getParamOrDefault("SacValid", "Land");
        final CardCollection junk = junk(ai, CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), ai, sa.getHostCard(), sa));
        int x = junk.size();
        if (cost == null && sa.getPayCosts() != null && sa.getPayCosts().getTotalMana() != null) {
            // Nahiri's Lithoforming pays X in mana too
            x = Math.min(x, forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai, false) - sa.getPayCosts().getTotalMana().getCMC());
        }
        if (x < 2) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        PLANNED.put(sa.getHostCard().getId(), new CardCollection(junk.subList(0, x)));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Immoral Bargain: X of its junk creatures for X of their best nonland permanents
    private static AiAbilityDecision sacXForX(final Player ai, final SpellAbility sa) {
        final CostSacrifice cost = sacCost(sa);
        if (cost == null || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final CardCollection junk = junk(ai, CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), ai, sa.getHostCard(), sa));
        sa.setXManaCostPaid(Math.max(1, junk.size()));
        sa.resetTargets();
        final List<Card> theirs = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa),
                c -> c.getController().isOpponentOf(ai) && c.canBeDestroyed()));
        theirs.sort(Comparator.comparingInt(SacrificePayoffAi::worth).reversed());
        junk.sort(Comparator.comparingInt(SacrificePayoffAi::worth));
        int x = 0;
        int v = 0;
        while (x < junk.size() && x < theirs.size() && worth(theirs.get(x)) > worth(junk.get(x)) + 40) {
            v += worth(theirs.get(x)) - worth(junk.get(x));
            x++;
        }
        if (x < 1 || v < 100) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        sa.resetTargets();
        theirs.stream().limit(x).forEach(c -> sa.getTargets().add(c));
        PLANNED.put(sa.getHostCard().getId(), new CardCollection(junk.subList(0, x)));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // The Martyrs: X cards of a color revealed from hand, the creature itself sacrificed
    private static AiAbilityDecision revealX(final Player ai, final SpellAbility sa) {
        final forge.game.cost.CostReveal reveal = costPart(sa, forge.game.cost.CostReveal.class);
        if (reveal == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        final int max = CardLists.getValidCards(ai.getCardsIn(ZoneType.Hand), reveal.getType().split(";"), ai, host, sa).size();
        if (max < 1) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final int body = ComputerUtilCard.evaluateCreature(host);
        int x = 0;
        switch (sa.getApi()) {
            case GainLife -> {
                // Martyr of Sands: three times X, when the life matters
                if (ComputerUtil.aiLifeInDanger(ai, false, 0) || ai.getLife() <= 5) {
                    x = max;
                }
            }
            case Counter -> {
                // Martyr of Frost: counter unless they pay X - more than they have open
                if (sa.usesTargeting() && !ai.getGame().getStack().isEmpty()) {
                    final SpellAbility top = ai.getGame().getStack().peekAbility();
                    if (top.getActivatingPlayer().isOpponentOf(ai) && sa.canTargetSpellAbility(top)
                            && top.getHostCard().getCMC() >= 3) {
                        final int open = forge.ai.ComputerUtilMana.getAvailableManaEstimate(top.getActivatingPlayer(), false);
                        if (open < max) {
                            x = open + 1;
                            sa.resetTargets();
                            sa.getTargets().add(top);
                        }
                    }
                }
            }
            case DamageAll -> {
                // Martyr of Ashes: the X that does the most to the creatures it hits
                int best = 0;
                for (int n = 1; n <= max; n++) {
                    int v = -body;
                    for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa.getParamOrDefault("ValidCards", "Creature"),
                            ai, host, sa)) {
                        if (!c.equals(host) && n >= c.getLethalDamage() && c.canBeDestroyed()) {
                            v += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                    if (v > best) {
                        best = v;
                        x = n;
                    }
                }
                if (best < 100) {
                    x = 0;
                }
            }
            default -> { }
        }
        if (x < 1) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Disciple of Bolas: "sacrifice another creature" as an effect, the payoff by what was sacrificed - the creature
    // that buys the most for what it's worth, and when it's a trigger it must happen anyway
    private static AiAbilityDecision sacrificeEffect(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield),
                sa.getParamOrDefault("SacValid", "Creature").split(","), ai, host, sa);
        valid.remove(host);
        int best = Integer.MIN_VALUE;
        Card bestCard = null;
        for (final Card c : valid) {
            final int v = payoff(ai, sa.getSubAbility(), List.of(c)) - worth(c);
            if (v > best) {
                best = v;
                bestCard = c;
            }
        }
        if (bestCard == null) {
            return new AiAbilityDecision(sa.isTrigger() ? 100 : 0, sa.isTrigger() ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        }
        PLANNED.put(host.getId(), new CardCollection(bestCard));
        return best >= 0 || sa.isTrigger() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    // Corpse Lunge: X is the exiled creature card's power, so the card from its graveyard that does the most
    private static AiAbilityDecision exileFor(final Player ai, final SpellAbility sa) {
        final forge.game.cost.CostExile exile = costPart(sa, forge.game.cost.CostExile.class);
        if (exile == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        int best = Integer.MIN_VALUE;
        Card bestCard = null;
        for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Graveyard), exile.getType().split(";"), ai, host, sa)) {
            final int v = payoff(ai, sa, List.of(c)) - 10;
            if (v > best) {
                best = v;
                bestCard = c;
            }
        }
        if (bestCard == null || best < (sa.isSpell() ? 60 : 30)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        payoff(ai, sa, List.of(bestCard));
        PLANNED.put(host.getId(), new CardCollection(bestCard));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Harvest Pyre, Chill Haunting, Haunting Misery: X cards exiled from its graveyard, so X is what the kill takes
    private static AiAbilityDecision exileForX(final Player ai, final SpellAbility sa) {
        final forge.game.cost.CostExile exile = costPart(sa, forge.game.cost.CostExile.class);
        if (exile == null || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final int available = CardLists.getValidCards(ai.getCardsIn(ZoneType.Graveyard), exile.getType().split(";"), ai, sa.getHostCard(), sa).size();
        sa.resetTargets();
        int x = 0;
        forge.game.GameEntity target = null;
        int value = 0;
        if (sa.getApi() == ApiType.DealDamage || sa.getApi() == ApiType.Pump) {
            final boolean damage = sa.getApi() == ApiType.DealDamage;
            for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
                final int need = damage ? c.getLethalDamage() : c.getNetToughness();
                if (c.getController().isOpponentOf(ai) && need <= available && need > 0 && (!damage || c.canBeDestroyed())) {
                    final int v = ComputerUtilCard.evaluateCreature(c);
                    if (v > value) {
                        value = v;
                        target = c;
                        x = need;
                    }
                }
            }
            if (damage) {
                for (final Player p : ai.getOpponents()) {
                    // Haunting Misery: the player, for lethal or a real chunk
                    if (sa.canTarget(p) && available > 0 && p.canLoseLife()) {
                        final boolean lethal = available >= p.getLife() && !p.cantLoseForZeroOrLessLife();
                        final int v = lethal ? 1000 : available >= 4 ? available * 15 : 0;
                        if (v > value) {
                            value = v;
                            target = p;
                            x = lethal ? p.getLife() : available;
                        }
                    }
                }
            }
        }
        if (target == null || value < 60) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        sa.getTargets().add(target);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision sacrificeFor(final Player ai, final SpellAbility sa) {
        if ("RevealFor".equals(sa.getParam("AILogic"))) {
            return revealFor(ai, sa);
        }
        final Card host = sa.getHostCard();
        final CostSacrifice cost = sacCost(sa);
        if (cost == null && sa.getApi() == ApiType.Sacrifice) {
            return sacrificeEffect(ai, sa);
        }
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
            // a card marked to be sacrificed costs nothing to give up
            final int lost = free ? 0 : sac.stream().filter(c -> !c.hasSVar("SacMe")).mapToInt(SacrificePayoffAi::worth).sum();
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

    // Sacrifice, Burnt Offering, Metamorphosis: the mana is the sacrificed creature's mana value, so the most mana from
    // a creature it can spare (junk, or a small one), when that mana casts something it otherwise couldn't
    private static AiAbilityDecision sacRitual(final Player ai, final SpellAbility sa) {
        final CostSacrifice cost = sacCost(sa);
        if (cost == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), ai, host, sa);
        valid.remove(host);
        final CardCollection spare = junk(ai, valid);
        for (final Card c : valid) {
            if (!spare.contains(c) && worth(c) <= 130) {
                spare.add(c);
            }
        }
        // Metamorphosis makes one more
        final int plus = host.getSVar("X").contains("Plus.1") ? 1 : 0;
        final List<Card> order = new java.util.ArrayList<>(spare);
        order.sort(Comparator.comparingInt((Card c) -> c.getCMC()).reversed());
        for (final Card c : order) {
            if (c.getCMC() + plus < 2) {
                break;
            }
            if (ManaAi.doManaRitualLogic(ai, sa, false, c.getCMC() + plus)) {
                PLANNED.put(host.getId(), new CardCollection(c));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    // Impelled Giant: once blocks are known, tap its idle red creature whose power makes the attack lethal or wins the fight
    private static AiAbilityDecision tapFor(final Player ai, final SpellAbility sa) {
        final forge.game.combat.Combat combat = ai.getGame().getCombat();
        final Card host = sa.getHostCard();
        if (combat == null || !combat.isAttacking(host) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        final forge.game.cost.CostTapType cost = costPart(sa, forge.game.cost.CostTapType.class);
        if (cost == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final List<Card> blockers = combat.getBlockers(host);
        final Player opp = combat.getDefenderPlayerByAttacker(host);
        Card best = null;
        for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), ai, host, sa)) {
            if (c.equals(host) || c.isTapped() || combat.isAttacking(c) || combat.isBlocking(c)) {
                continue;
            }
            final int p = c.getNetPower();
            boolean good = false;
            if ((blockers.isEmpty() || host.hasKeyword(forge.game.keyword.Keyword.TRAMPLE)) && opp != null) {
                final int need = forge.ai.ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
                good = need > 0 && p >= need;
            }
            if (blockers.size() == 1) {
                final Card b = blockers.get(0);
                good |= host.getNetCombatDamage() < b.getLethalDamage() && host.getNetCombatDamage() + p >= b.getLethalDamage();
            }
            if (good && (best == null || p < best.getNetPower())) {
                best = c;
            }
        }
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        PLANNED.put(host.getId(), new CardCollection(best));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Reshape: its least valuable artifact for the best artifact its library has within the X it can pay
    private static AiAbilityDecision reshape(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final CostSacrifice cost = sacCost(sa);
        if (cost == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), cost.getTypeFor(ai).split(";"), ai, host, sa);
        final Card give = valid.isEmpty() ? null : valid.stream().min(java.util.Comparator.comparingInt(SacrificePayoffAi::worth)).orElse(null);
        final forge.card.mana.ManaCost mana = sa.getPayCosts().getTotalMana();
        final int x = forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai, false) - mana.getCMC();
        final Card get = ai.getCardsIn(ZoneType.Library).stream().filter(c -> c.isArtifact() && c.getCMC() <= x)
                .max(java.util.Comparator.comparingInt(SacrificePayoffAi::worth)).orElse(null);
        if (give == null || get == null || worth(get) - worth(give) < 100) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(get.getCMC());
        PLANNED.put(host.getId(), new CardCollection(give));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Nahiri's Wrath: X discards whose total mana value kills up to X of their creatures, the most value for the cards
    private static AiAbilityDecision discardMv(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final List<Card> hand = new ArrayList<>(CardLists.filter(ai.getCardsIn(ZoneType.Hand), c -> !c.equals(host) && !c.isLand()));
        // the cards that give the most mana value for what they're worth
        hand.sort(Comparator.comparingInt((Card c) -> worth(c) - c.getCMC() * 40));
        final List<Card> theirs = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai)));
        theirs.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        int best = 0;
        List<Card> bestDiscard = null;
        List<Card> bestTargets = null;
        int total = 0;
        for (int x = 1; x <= Math.min(3, hand.size()); x++) {
            total += hand.get(x - 1).getCMC();
            final int dmg = total;
            final List<Card> kills = CardLists.filter(theirs, c -> c.isCreature() && dmg >= c.getLethalDamage() && c.canBeDestroyed());
            final List<Card> picks = kills.subList(0, Math.min(x, kills.size()));
            final int v = picks.stream().mapToInt(ComputerUtilCard::evaluateCreature).sum()
                    - hand.subList(0, x).stream().mapToInt(c -> 30 + c.getCMC() * 15).sum();
            if (v > best) {
                best = v;
                bestDiscard = new ArrayList<>(hand.subList(0, x));
                bestTargets = new ArrayList<>(picks);
            }
        }
        if (bestDiscard == null || best < 120) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestDiscard.size());
        sa.resetTargets();
        bestTargets.forEach(c -> sa.getTargets().add(c));
        PLANNED.put(host.getId(), new CardCollection(bestDiscard));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // what it can give up for nothing much: what's about to die anyway, cheap tokens, lands past what its hand needs,
    // and cards marked to be sacrificed
    private static CardCollection junk(final Player ai, final CardCollection valid) {
        final List<GameObject> doomed = ai.getGame().getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
        int need = 5;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.isLand()) {
                need = Math.max(need, c.getCMC());
            }
        }
        final CardCollection out = new CardCollection();
        int lands = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), forge.game.card.CardPredicates.LANDS);
        for (final Card c : valid) {
            if (doomed.contains(c) || c.hasSVar("SacMe") || c.isToken() && worth(c) < 100) {
                out.add(c);
            } else if (c.isLand() && lands > need) {
                out.add(c);
                lands--;
            }
        }
        return out;
    }

    // Reprocess, God-Eternal Bontu, Renounce: "sacrifice any number" for a card or life each - its junk
    private static AiAbilityDecision sacrificeForEach(final Player ai, final SpellAbility sa, final CardCollection valid) {
        final SpellAbility next = sa.getSubAbility();
        final CardCollection junk = junk(ai, valid);
        // Kylox, Visionary Inventor: exiling cards to cast free is a draw when its library is mostly spells it can cast
        final boolean draws = next.getApi() == ApiType.Draw || next.getApi() == ApiType.Dig && CardLists.count(ai.getCardsIn(ZoneType.Library),
                c -> c.isInstant() || c.isSorcery()) * 3 >= ai.getCardsIn(ZoneType.Library).size();
        final boolean danger = ComputerUtil.aiLifeInDanger(ai, false, 0);
        if (junk.size() < 2 || !draws && !danger) {
            // a mandatory trigger sacrifices nothing
            PLANNED.put(sa.getHostCard().getId(), new CardCollection());
            return new AiAbilityDecision(sa.isTrigger() ? 100 : 0, sa.isTrigger() ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        }
        PLANNED.put(sa.getHostCard().getId(), junk);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision sacrificeAny(final Player ai, final SpellAbility sa) {
        if (sa.getApi() == ApiType.Sacrifice && sa.getSubAbility() != null
                && (sa.getSubAbility().getApi() == ApiType.Draw || sa.getSubAbility().getApi() == ApiType.GainLife
                || sa.getSubAbility().getApi() == ApiType.Dig)) {
            final CardCollection valid = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield),
                    sa.getParamOrDefault("SacValid", "Permanent").split(","), ai, sa.getHostCard(), sa);
            valid.remove(sa.getHostCard());
            return sacrificeForEach(ai, sa, valid);
        }
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
