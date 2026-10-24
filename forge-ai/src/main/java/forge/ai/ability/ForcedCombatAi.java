package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Cards that make creatures attack or block, judged by the combat they force.
 * <ul>
 * <li>AILogic$ ForceAttack (Alluring Siren, Imps' Taunt, Nettling Imp, Siren's Call, Taunt...): at the opponent's
 * beginning of combat (or for their next turn), their creatures that would attack into a blocker that kills them and
 * lives, or that can't attack and would be destroyed for it.</li>
 * <li>AILogic$ ForceBlock (Culling Mark, Provoke, Hunt Down, Mark for Death): a creature made to block one of the
 * AI's attackers, whichever block is worst for the AI; Mark for Death also counts the attack its "other creatures can't
 * block" lets through.</li>
 * <li>AILogic$ Lure (Alluring Scent, Bloodscent, Roar of Challenge): every blocker that can goes to one creature -
 * worth it if the rest of the attack is then lethal, or the lured creature lives through it and kills.</li>
 * <li>AILogic$ MustBeBlocked (Irresistible Prey): one blocker the opponent picks, so the worst of them.</li>
 * </ul>
 */
public final class ForcedCombatAi {

    private static final String LURE = "All creatures able to block CARDNAME do so.";

    // the creature type Walking Desecration meant when it decided, for the choice at resolution
    private static final Map<Integer, String> CHOSEN_TYPES = new ConcurrentHashMap<>();

    private ForcedCombatAi() { }

    public static boolean handles(final String logic) {
        return "ForceAttack".equals(logic) || "ForceBlock".equals(logic) || "Lure".equals(logic) || "MustBeBlocked".equals(logic);
    }

    public static String chosenType(final Card host) {
        return host == null ? null : CHOSEN_TYPES.get(host.getId());
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "ForceAttack" -> forceAttack(ai, sa);
            case "ForceBlock" -> forceBlock(ai, sa);
            default -> lure(ai, sa, "MustBeBlocked".equals(sa.getParam("AILogic")));
        };
    }

    private static List<SpellAbility> chain(final SpellAbility sa) {
        final List<SpellAbility> out = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            out.add(sub);
        }
        return out;
    }

    private static SpellAbility firstTargeting(final SpellAbility sa) {
        return chain(sa).stream().filter(SpellAbility::usesTargeting).findFirst().orElse(null);
    }

    private static int threshold(final SpellAbility sa) {
        if (!sa.isSpell()) {
            return 60;
        }
        // a cantrip gives the card back
        return chain(sa).stream().anyMatch(s -> s.getApi() == ApiType.Draw) ? 60 : 100;
    }

    private static int eval(final Card c) {
        return ComputerUtilCard.evaluateCreature(c);
    }

    private static int damage(final Card attacker, final Player defender) {
        return ComputerUtilCombat.predictDamageTo(defender, attacker.getNetCombatDamage(), attacker, true);
    }

    private static boolean sorcerySpeed(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        return sa.isSpell() && !host.isInstant() && !host.hasKeyword(Keyword.FLASH);
    }

    private static AiAbilityDecision play(final int value, final int threshold) {
        return value >= threshold ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
    }

    // ---- creatures made to attack ----

    private static AiAbilityDecision forceAttack(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        // Taunt sets up the opponent's next turn; everything else is used as their combat begins
        final boolean nextTurn = sa.getApi() == ApiType.DelayedTrigger;
        // Maddening Imp can't wait that long
        final PhaseType when = "Upkeep->Main1".equals(sa.getParam("ActivationPhases")) ? PhaseType.MAIN1 : PhaseType.COMBAT_BEGIN;
        if (nextTurn ? !ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2) : ph.isPlayerTurn(ai) || !ph.is(when)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        // destroyed at end of turn if it didn't attack (Nettling Imp, Siren's Call)
        final boolean punishing = !nextTurn && chain(sa).stream().anyMatch(s -> s.getApi() == ApiType.DelayedTrigger);
        final SpellAbility tgt = firstTargeting(sa);

        int best = Integer.MIN_VALUE;
        Object bestPick = null;
        final List<Object> picks = new ArrayList<>();
        if (tgt != null) {
            tgt.resetTargets();
            for (final Player p : game.getPlayers()) {
                if (p.isOpponentOf(ai) && tgt.canTarget(p)) {
                    picks.add(p);
                }
            }
            for (final Card c : CardUtil.getValidCardsToTarget(tgt)) {
                if (c.getController().isOpponentOf(ai)) {
                    picks.add(c);
                }
            }
        } else if (sa.getApi() == ApiType.ChooseType) {
            for (final Card c : ph.getPlayerTurn().getCreaturesInPlay()) {
                for (final String type : c.getType().getCreatureTypes()) {
                    if (!picks.contains(type)) {
                        picks.add(type);
                    }
                }
            }
        } else {
            picks.add(ph.getPlayerTurn());
        }
        for (final Object pick : picks) {
            final CardCollection forced;
            if (pick instanceof Card c) {
                forced = new CardCollection(c);
            } else if (pick instanceof Player p) {
                forced = new CardCollection(p.getCreaturesInPlay());
            } else {
                forced = CardLists.filter(ph.getPlayerTurn().getCreaturesInPlay(), c -> c.getType().hasCreatureType((String) pick));
            }
            final int v = forcedAttackValue(ai, forced, nextTurn, punishing);
            if (v > best) {
                best = v;
                bestPick = pick;
            }
        }
        if (bestPick == null || best < threshold(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        if (bestPick instanceof GameEntity e && tgt != null) {
            tgt.getTargets().add(e);
        } else if (bestPick instanceof String type) {
            CHOSEN_TYPES.put(sa.getHostCard().getId(), type);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static int forcedAttackValue(final Player ai, final CardCollection forced, final boolean nextTurn, final boolean punishing) {
        // the AI's creatures stay as they are through the opponent's turn
        final CardCollection blockers = CardLists.filter(ai.getCreaturesInPlay(), CombatUtil::canBlock);
        int value = 0;
        int unanswered = 0;
        final List<Card> sorted = new ArrayList<>(forced);
        sorted.sort(Comparator.comparingInt(ForcedCombatAi::eval).reversed());
        for (final Card f : sorted) {
            if (!f.isCreature() || !f.getController().isOpponentOf(ai)) {
                continue;
            }
            final boolean able = nextTurn ? CombatUtil.canAttackNextTurn(f, ai) : CombatUtil.canAttack(f, ai);
            if (!able) {
                if (punishing && !f.isFirstTurnControlled() && !f.getType().hasCreatureType("Wall") && f.canBeDestroyed()) {
                    value += eval(f);
                }
                continue;
            }
            final Card answer = blockers.stream().filter(b -> CombatUtil.canBlock(f, b, nextTurn)
                    && ComputerUtilCombat.canDestroyAttacker(ai, f, b, null, false)
                    && !ComputerUtilCombat.canDestroyBlocker(ai, b, f, null, false)).findFirst().orElse(null);
            if (answer != null) {
                value += eval(f);
                blockers.remove(answer);
            } else {
                unanswered += damage(f, ai);
            }
        }
        if (unanswered > 0 && ai.canLoseLife()) {
            // with whatever else they can send
            int incoming = unanswered;
            for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
                if (!forced.contains(c) && (nextTurn ? CombatUtil.canAttackNextTurn(c, ai) : CombatUtil.canAttack(c, ai))) {
                    incoming += damage(c, ai);
                }
            }
            if (incoming >= ai.getLife()) {
                value -= 1000;
            }
        }
        return value;
    }

    // ---- creatures made to block ----

    private static List<Card> attackers(final Player ai, final Player opp, final boolean planned) {
        final Combat combat = ai.getGame().getCombat();
        if (!planned && combat != null) {
            return new ArrayList<>(combat.getAttackers());
        }
        return CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
    }

    // when the attack is still to be declared, or else the attack as declared
    private static Boolean planned(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai)) {
            return null;
        }
        if (ph.is(PhaseType.MAIN1)) {
            return true;
        }
        final Combat combat = ai.getGame().getCombat();
        if (!sorcerySpeed(sa) && ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && combat != null && !combat.getAttackers().isEmpty()) {
            return false;
        }
        return null;
    }

    private static int blockOutcome(final Player ai, final Card attacker, final Card blocker) {
        int v = 0;
        if (ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker, null, false)) {
            v += eval(blocker);
        }
        if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker, null, false)) {
            v -= eval(attacker);
        }
        return v;
    }

    private static AiAbilityDecision forceBlock(final Player ai, final SpellAbility sa) {
        final Boolean planned = planned(ai, sa);
        if (planned == null) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final Player opp = planned ? ai.getWeakestOpponent()
                : ai.getGame().getCombat().getDefenderPlayerByAttacker(ai.getGame().getCombat().getAttackers().get(0));
        if (opp == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final List<SpellAbility> tgts = chain(sa).stream().filter(SpellAbility::usesTargeting).toList();
        if (tgts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // Hunt Down names the attacker too; the blocker is the last target either way
        final SpellAbility blockerSa = tgts.get(tgts.size() - 1);
        final SpellAbility attackerSa = tgts.size() > 1 ? tgts.get(0) : null;
        // Provoke untaps it first
        final boolean untaps = chain(sa).stream().anyMatch(s -> s.getApi() == ApiType.Untap);
        // Mark for Death: other creatures that player controls can't block
        final boolean alone = chain(sa).stream().anyMatch(s -> s.getApi() == ApiType.Effect
                && java.util.Arrays.stream(s.getParamOrDefault("StaticAbilities", "").split(","))
                        .anyMatch(n -> s.getSVar(n).contains("can't block")));
        final List<Card> attackers = attackers(ai, opp, planned);
        if (attackers.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        int best = Integer.MIN_VALUE;
        Card bestBlocker = null;
        Card bestAttacker = null;
        for (final SpellAbility t : tgts) {
            t.resetTargets();
        }
        final List<Card> attackerOptions = new ArrayList<>();
        if (attackerSa != null) {
            attackerOptions.addAll(CardLists.filter(attackers, attackerSa::canTarget));
        } else {
            attackerOptions.add(null);
        }
        for (final Card t : CardUtil.getValidCardsToTarget(blockerSa)) {
            if (!t.getController().isOpponentOf(ai)) {
                continue;
            }
            for (final Card a : attackerOptions) {
                // an attack still to be declared leaves out what this blocker would kill
                final List<Card> able = CardLists.filter(a != null ? List.of(a) : attackers, x -> CombatUtil.canBlock(x, t, untaps)
                        && (!planned || !ComputerUtilCombat.canDestroyAttacker(ai, x, t, null, false)));
                if (able.isEmpty()) {
                    continue;
                }
                // they pick which of the attackers it blocks
                int v = able.stream().mapToInt(x -> blockOutcome(ai, x, t)).min().getAsInt();
                if (alone && opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife()) {
                    final int total = attackers.stream().mapToInt(x -> damage(x, opp)).sum();
                    final int stopped = able.stream().mapToInt(x -> damage(x, opp)).max().getAsInt();
                    if (total - stopped >= opp.getLife()) {
                        v += 1000;
                    }
                }
                if (v > best) {
                    best = v;
                    bestBlocker = t;
                    bestAttacker = a;
                }
            }
        }
        if (bestBlocker == null || best < threshold(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        if (attackerSa != null) {
            attackerSa.getTargets().add(bestAttacker);
        }
        blockerSa.getTargets().add(bestBlocker);
        return play(best, threshold(sa));
    }

    // ---- lures ----

    // unblocked damage if each blocker takes the biggest attacker it can
    private static int assault(final List<Card> attackers, final List<Card> blockers, final Player opp) {
        final List<Card> left = new ArrayList<>(attackers);
        left.sort(Comparator.comparingInt((Card c) -> damage(c, opp)).reversed());
        for (final Card b : blockers) {
            left.stream().filter(a -> CombatUtil.canBlock(a, b)).findFirst().ifPresent(left::remove);
        }
        return left.stream().mapToInt(c -> damage(c, opp)).sum();
    }

    private static AiAbilityDecision lure(final Player ai, final SpellAbility sa, final boolean justOne) {
        final Boolean planned = planned(ai, sa);
        if (planned == null) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final Combat combat = ai.getGame().getCombat();
        final Player opp = planned ? ai.getWeakestOpponent() : combat.getDefenderPlayerByAttacker(combat.getAttackers().get(0));
        final SpellAbility tgt = firstTargeting(sa);
        if (opp == null || tgt == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final List<Card> attackers = attackers(ai, opp, planned);
        final CardCollection blockers = CardLists.filter(opp.getCreaturesInPlay(), CombatUtil::canBlock);
        final boolean canLose = opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife();
        final int before = assault(attackers, blockers, opp);
        // Roar of Challenge: indestructible with ferocious
        final boolean grantsIndestructible = chain(sa).stream().anyMatch(s -> s.getApi() == ApiType.Pump
                && s.getParamOrDefault("KW", "").contains("Indestructible") && s.metConditions());

        tgt.resetTargets();
        int best = Integer.MIN_VALUE;
        Card bestLure = null;
        for (final Card l : attackers) {
            if (!tgt.canTarget(l)) {
                continue;
            }
            final List<Card> able = CardLists.filter(blockers, b -> CombatUtil.canBlock(l, b));
            if (able.isEmpty()) {
                continue;
            }
            int v;
            if (justOne) {
                v = able.stream().mapToInt(b -> blockOutcome(ai, l, b)).min().getAsInt();
            } else {
                final List<Card> rest = new ArrayList<>(attackers);
                rest.remove(l);
                final List<Card> free = new ArrayList<>(blockers);
                free.removeAll(able);
                if (canLose && before < opp.getLife() && assault(rest, free, opp) >= opp.getLife()) {
                    v = 1000;
                } else {
                    // it deals its damage out among them, the easiest kills first
                    final List<Card> order = new ArrayList<>(able);
                    order.sort(Comparator.comparingInt(Card::getLethalDamage));
                    int power = l.getNetCombatDamage();
                    int killed = 0;
                    int incoming = 0;
                    boolean deathtouch = false;
                    for (final Card b : order) {
                        final int need = l.hasKeyword(Keyword.DEATHTOUCH) ? 1 : b.getLethalDamage();
                        if (power >= need && b.canBeDestroyed()) {
                            power -= need;
                            killed += eval(b);
                        }
                        incoming += b.getNetCombatDamage();
                        deathtouch |= b.hasKeyword(Keyword.DEATHTOUCH) && b.getNetCombatDamage() > 0;
                    }
                    final boolean lives = grantsIndestructible || l.hasKeyword(Keyword.INDESTRUCTIBLE)
                            || !deathtouch && incoming < l.getLethalDamage();
                    v = lives ? killed : killed - eval(l);
                }
            }
            if (v > best) {
                best = v;
                bestLure = l;
            }
        }
        if (bestLure == null || best < threshold(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        tgt.getTargets().add(bestLure);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** For the attack AI: the blockers a lured attacker takes up. */
    public static boolean isLure(final Card attacker) {
        return attacker.hasKeyword(LURE);
    }
}
