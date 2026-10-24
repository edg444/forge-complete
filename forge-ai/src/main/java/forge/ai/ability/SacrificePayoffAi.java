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
        return "SacrificeFor".equals(logic) || "SacrificeAny".equals(logic) || "RevealFor".equals(logic) || "ExileForX".equals(logic)
                || "ExileFor".equals(logic) || "RevealX".equals(logic);
    }

    /** What the AI decided to sacrifice for this card, if anything. */
    public static CardCollection planned(final SpellAbility sa) {
        if (sa == null || sa.getHostCard() == null || !handles(sa.getRootAbility().getParam("AILogic"))) {
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
                    // Sanguine Praetor: each creature with the sacrificed creature's mana value
                    final int n = sac.isEmpty() ? -1 : sac.get(0).getCMC();
                    for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                        if (c.isCreature() && !sac.contains(c) && c.getCMC() == n && c.canBeDestroyed()) {
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
