package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Smaller whole-card logics.
 * <ul>
 * <li>AILogic$ BounceBoth (Peel from Reality, Rite of Undoing, Run Away Together): one of its own and one of theirs back
 * to hand - its own one about to be destroyed, or one with an enters trigger, against their best.</li>
 * <li>AILogic$ GraveShuffle (Krosan Reclamation, Memory's Journey, Stream of Consciousness): cards shuffled out of a
 * graveyard - their flashback and other graveyard-castable cards and big reanimation targets, or its own best ones when
 * its library is running out.</li>
 * <li>AILogic$ RemoveCombatant (Reconnaissance, Labyrinth of Skophos): out of combat - their biggest attacker, a
 * blocker that would kill its attacker, or its own attacker that would die for nothing.</li>
 * <li>AILogic$ CashIn (Midsummer Revel, Recantation, Rumbling Crescendo): sacrificed once enough verse counters are on
 * it.</li>
 * </ul>
 */
public final class MiscLogicAi {

    private MiscLogicAi() { }

    public static boolean handles(final String logic) {
        return "BounceBoth".equals(logic) || "GraveShuffle".equals(logic) || "RemoveCombatant".equals(logic) || "CashIn".equals(logic)
                || "WipeWithSub".equals(logic) || "RecycleForDamage".equals(logic) || "EchoDestroy".equals(logic)
                || "SelfDamage".equals(logic) || "BounceWithAuras".equals(logic) || "PayLifePump".equals(logic)
                || "DrawOnDamage".equals(logic) || "DamageKill".equals(logic) || "LethalOnly".equals(logic)
                || "LifeForGraveyard".equals(logic) || "LandLock".equals(logic) || "LifeFromPower".equals(logic)
                || "LifePerTarget".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "BounceBoth" -> bounceBoth(ai, sa);
            case "GraveShuffle" -> graveShuffle(ai, sa);
            case "RemoveCombatant" -> removeCombatant(ai, sa);
            case "WipeWithSub" -> wipeWithSub(ai, sa);
            case "RecycleForDamage" -> recycleForDamage(ai, sa);
            case "EchoDestroy" -> echoDestroy(ai, sa);
            case "SelfDamage" -> selfDamage(ai, sa);
            case "BounceWithAuras" -> bounceWithAuras(ai, sa);
            case "PayLifePump" -> payLifePump(ai, sa);
            case "DrawOnDamage" -> drawOnDamage(ai, sa);
            case "DamageKill" -> damageKill(ai, sa);
            case "LethalOnly" -> lethalOnly(ai, sa);
            case "LifeForGraveyard" -> lifeForGraveyard(ai, sa);
            case "LandLock" -> landLock(ai, sa);
            case "LifeFromPower" -> lifeFromPower(ai, sa);
            case "LifePerTarget" -> lifePerTarget(ai, sa);
            default -> cashIn(ai, sa);
        };
    }

    private static AiAbilityDecision no() {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision yes() {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static List<SpellAbility> targeting(final SpellAbility sa) {
        final List<SpellAbility> out = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                out.add(sub);
            }
        }
        return out;
    }

    private static boolean hasEtb(final Card c) {
        for (final forge.game.trigger.Trigger t : c.getTriggers()) {
            if (t.getMode() == forge.game.trigger.TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                    && t.getParamOrDefault("ValidCard", "").startsWith("Card.Self")) {
                return true;
            }
        }
        return false;
    }

    // ---- bounce one of each ----

    private static int ownBounce(final Card c, final List<GameObject> doomed) {
        if (c.isToken()) {
            return -1000;
        }
        if (doomed.contains(c)) {
            return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
        }
        return (hasEtb(c) ? 40 : 0) - c.getCMC() * 10;
    }

    private static int theirBounce(final Card c) {
        final int v = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
        // a token is gone for good, a card only loses its mana and Auras
        return c.isToken() ? v : v / 3 + c.getCMC() * 15 + c.getAttachedCards().size() * 40;
    }

    private static AiAbilityDecision bounceBoth(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean answering = !ai.getGame().getStack().isEmpty();
        final List<GameObject> doomed = answering ? ComputerUtil.predictThreatenedObjects(ai, null) : List.of();
        if (!answering && !(ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1)) && !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai))) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final List<SpellAbility> tgts = targeting(sa);
        tgts.forEach(SpellAbility::resetTargets);
        Card mine = null;
        Card theirs = null;
        int mineV = Integer.MIN_VALUE;
        int theirsV = Integer.MIN_VALUE;
        for (final SpellAbility t : tgts) {
            for (final Card c : CardUtil.getValidCardsToTarget(t)) {
                if (c.getController().equals(ai) && ownBounce(c, doomed) > mineV) {
                    mineV = ownBounce(c, doomed);
                    mine = c;
                } else if (c.getController().isOpponentOf(ai) && theirBounce(c) > theirsV) {
                    theirsV = theirBounce(c);
                    theirs = c;
                }
            }
        }
        if (mine == null || theirs == null || mineV + theirsV < (answering ? 60 : 120)) {
            return no();
        }
        if (tgts.size() == 1) {
            // Run Away Together: both in one targeting
            tgts.get(0).getTargets().add(mine);
            tgts.get(0).getTargets().add(theirs);
        } else {
            boolean mineDone = false;
            boolean theirsDone = false;
            for (final SpellAbility t : tgts) {
                if (!mineDone && t.canTarget(mine)) {
                    t.getTargets().add(mine);
                    mineDone = true;
                } else if (!theirsDone && t.canTarget(theirs)) {
                    t.getTargets().add(theirs);
                    theirsDone = true;
                }
            }
            if (!mineDone || !theirsDone) {
                tgts.forEach(SpellAbility::resetTargets);
                return no();
            }
        }
        return yes();
    }

    // ---- shuffle graveyard cards away ----

    private static int graveValue(final Card c) {
        int v = 0;
        if (c.hasKeyword(Keyword.FLASHBACK) || c.hasKeyword(Keyword.ESCAPE) || c.hasKeyword(Keyword.UNEARTH)
                || c.hasKeyword(Keyword.DISTURB) || c.hasKeyword(Keyword.EMBALM) || c.hasKeyword(Keyword.ETERNALIZE)) {
            v += 60 + c.getCMC() * 10;
        }
        if (c.isCreature() && c.getCMC() >= 5) {
            v += 40 + c.getCMC() * 5;
        }
        return v;
    }

    private static AiAbilityDecision graveShuffle(final Player ai, final SpellAbility sa) {
        final List<SpellAbility> tgts = targeting(sa);
        if (tgts.size() != 2) {
            return no();
        }
        final SpellAbility player = tgts.get(0);
        final SpellAbility cards = tgts.get(1);
        player.resetTargets();
        cards.resetTargets();
        final int max = cards.getMaxTargets();
        Player who = null;
        List<Card> pick = List.of();
        int best = 0;
        for (final Player p : ai.getGame().getPlayers()) {
            if (!player.canTarget(p)) {
                continue;
            }
            final List<Card> grave = new ArrayList<>(p.getCardsIn(ZoneType.Graveyard));
            final boolean own = p.equals(ai);
            // its own only to keep from decking
            if (own && ai.getCardsIn(ZoneType.Library).size() > 5) {
                continue;
            }
            grave.sort(Comparator.comparingInt((Card c) -> own ? c.getCMC() : graveValue(c)).reversed());
            final List<Card> chosen = new ArrayList<>(grave.subList(0, Math.min(max, grave.size())));
            final int v = own ? chosen.size() * 40 : chosen.stream().mapToInt(MiscLogicAi::graveValue).sum();
            if (!own) {
                chosen.removeIf(c -> graveValue(c) == 0);
            }
            if (v > best && !chosen.isEmpty()) {
                best = v;
                who = p;
                pick = chosen;
            }
        }
        if (who == null || best < 60) {
            return no();
        }
        player.getTargets().add(who);
        for (final Card c : pick) {
            if (cards.canTarget(c)) {
                cards.getTargets().add(c);
            }
        }
        return cards.getTargets().isEmpty() ? no() : yes();
    }

    // ---- out of combat ----

    private static AiAbilityDecision removeCombatant(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        sa.resetTargets();
        final List<Card> options = CardUtil.getValidCardsToTarget(sa);
        Card best = null;
        int bestV = 0;
        for (final Card c : options) {
            int v = 0;
            if (c.getController().isOpponentOf(ai) && combat.isAttacking(c)) {
                // their attacker: what it would do to the AI and its blockers
                if (combat.isUnblocked(c)) {
                    final int dmg = ComputerUtilCombat.damageIfUnblocked(c, ai, combat, false);
                    v = dmg >= ai.getLife() ? 1000 : dmg * (ComputerUtilCombat.lifeInDanger(ai, combat) ? 30 : 6);
                }
                for (final Card b : combat.getBlockers(c)) {
                    if (ComputerUtilCombat.canDestroyBlocker(ai, b, c, combat, false)) {
                        v += ComputerUtilCard.evaluateCreature(b);
                    }
                }
            } else if (c.getController().isOpponentOf(ai) && combat.isBlocking(c)) {
                // their blocker: the attackers it would kill
                for (final Card a : combat.getAttackersBlockedBy(c)) {
                    if (ComputerUtilCombat.canDestroyAttacker(ai, a, c, combat, false)) {
                        v += ComputerUtilCard.evaluateCreature(a);
                    }
                }
            } else if (c.getController().equals(ai) && combat.isAttacking(c) && !combat.isUnblocked(c)) {
                // its own attacker that would die without trading
                final boolean dies = combat.getBlockers(c).anyMatch(b -> ComputerUtilCombat.canDestroyAttacker(ai, c, b, combat, false));
                final boolean kills = combat.getBlockers(c).anyMatch(b -> ComputerUtilCombat.canDestroyBlocker(ai, b, c, combat, false));
                if (dies && !kills) {
                    v = ComputerUtilCard.evaluateCreature(c);
                }
            }
            if (v > bestV) {
                bestV = v;
                best = c;
            }
        }
        if (best == null || bestV < (sa.isSpell() ? 60 : 30)) {
            return no();
        }
        sa.getTargets().add(best);
        return yes();
    }

    // ---- Calming Verse: each DestroyAll of the chain counts, its own included ----

    private static AiAbilityDecision wipeWithSub(final Player ai, final SpellAbility sa) {
        int v = 0;
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s.getApi() != forge.game.ability.ApiType.DestroyAll) {
                continue;
            }
            for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), s.getParam("ValidCards"), ai, sa.getHostCard(), s)) {
                if (c.canBeDestroyed()) {
                    final int w = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                    v += c.getController().isOpponentOf(ai) ? w : -w;
                }
            }
        }
        return v >= 100 ? yes() : no();
    }

    // ---- Dead Reckoning: a creature card back on top of the library, its power to a creature ----

    private static AiAbilityDecision recycleForDamage(final Player ai, final SpellAbility sa) {
        final SpellAbility dmg = sa.getSubAbility();
        if (!sa.usesTargeting() || dmg == null || !dmg.usesTargeting()) {
            return no();
        }
        sa.resetTargets();
        dmg.resetTargets();
        int best = 0;
        Card bestCard = null;
        Card bestTarget = null;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            for (final Card t : CardUtil.getValidCardsToTarget(dmg)) {
                if (t.getController().isOpponentOf(ai) && c.getNetPower() >= t.getLethalDamage() && t.canBeDestroyed()) {
                    final int v = ComputerUtilCard.evaluateCreature(t);
                    if (v > best) {
                        best = v;
                        bestCard = c;
                        bestTarget = t;
                    }
                }
            }
        }
        if (bestCard == null || best < 80) {
            return no();
        }
        sa.getTargets().add(bestCard);
        dmg.getTargets().add(bestTarget);
        return yes();
    }

    // ---- Echoing Calm, Echoing Ruin: the target and everything sharing its name ----

    private static AiAbilityDecision echoDestroy(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return no();
        }
        sa.resetTargets();
        int best = 0;
        Card bestT = null;
        for (final Card t : CardUtil.getValidCardsToTarget(sa)) {
            int v = 0;
            for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                if ((c.equals(t) || c.sharesNameWith(t) && c.sharesCardTypeWith(t)) && c.canBeDestroyed()) {
                    final int w = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                    v += c.getController().isOpponentOf(ai) ? w : -w;
                }
            }
            if (v > best) {
                best = v;
                bestT = t;
            }
        }
        if (bestT == null || best < 60) {
            return no();
        }
        sa.getTargets().add(bestT);
        return yes();
    }

    // ---- Inner Struggle: a creature deals its power to itself ----

    private static AiAbilityDecision selfDamage(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return no();
        }
        sa.resetTargets();
        final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(sa),
                c -> c.getController().isOpponentOf(ai) && c.getNetPower() >= c.getLethalDamage() && c.canBeDestroyed()));
        if (t == null) {
            return no();
        }
        sa.getTargets().add(t);
        return yes();
    }

    // ---- Hubris: a creature and its Auras back to hand ----

    private static AiAbilityDecision bounceWithAuras(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!sa.usesTargeting() || !ai.getGame().getStack().isEmpty()
                || !(ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1)) && !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai))) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        Card best = null;
        int bestV = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().isOpponentOf(ai)) {
                continue;
            }
            int v = theirBounce(c);
            for (final Card a : c.getAttachedCards()) {
                // their own Auras on it go back too
                v += a.getController().isOpponentOf(ai) ? 40 : -20;
            }
            if (v > bestV) {
                bestV = v;
                best = c;
            }
        }
        if (best == null || bestV < 120) {
            return no();
        }
        sa.getTargets().add(best);
        return yes();
    }

    // ---- Hatred: X life for +X/+0 on an unblocked attacker, just what makes the attack lethal ----

    private static AiAbilityDecision payLifePump(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !ai.equals(combat.getAttackingPlayer()) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        sa.resetTargets();
        for (final Card att : combat.getAttackers()) {
            final Player def = combat.getDefenderPlayerByAttacker(att);
            if (def == null || !combat.isUnblocked(att) || !sa.canTarget(att) || !def.canLoseLife() || def.cantLoseForZeroOrLessLife()) {
                continue;
            }
            final int coming = ComputerUtilCombat.sumDamageIfUnblocked(CardLists.filter(combat.getAttackers(), combat::isUnblocked), def);
            final int x = def.getLife() - coming;
            if (x > 0 && ai.getLife() > x + 2) {
                sa.setXManaCostPaid(x);
                sa.getTargets().add(att);
                return yes();
            }
        }
        return no();
    }

    // ---- Hunter's Insight: cards for its unblocked attacker's damage ----

    private static AiAbilityDecision drawOnDamage(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !ai.equals(combat.getAttackingPlayer()) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        sa.resetTargets();
        Card best = null;
        for (final Card att : combat.getAttackers()) {
            if (combat.isUnblocked(att) && sa.canTarget(att) && att.getNetCombatDamage() >= 2
                    && (best == null || att.getNetCombatDamage() > best.getNetCombatDamage())) {
                best = att;
            }
        }
        if (best == null) {
            return no();
        }
        sa.getTargets().add(best);
        return yes();
    }

    // ---- Burning Cloak, Goblin Cannon: the damage as removal, on a creature of theirs it kills ----

    private static AiAbilityDecision damageKill(final Player ai, final SpellAbility sa) {
        SpellAbility dmg = null;
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s.getApi() == forge.game.ability.ApiType.DealDamage) {
                dmg = s;
                break;
            }
        }
        if (dmg == null || !sa.usesTargeting()) {
            return no();
        }
        final int n = forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(), dmg.getParam("NumDmg"), dmg);
        sa.resetTargets();
        // Goblin Cannon sacrifices itself, so only for something worth it
        final boolean sacrifices = sa.getSubAbility() != null && sa.getSubAbility().getApi() == forge.game.ability.ApiType.Sacrifice;
        final int least = sacrifices ? ExchangeTradeAi.value(sa.getHostCard()) : 60;
        Card best = null;
        int bestV = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (c.getController().isOpponentOf(ai) && c.isCreature() && n >= c.getLethalDamage() && c.canBeDestroyed()) {
                final int v = ComputerUtilCard.evaluateCreature(c);
                if (v > bestV) {
                    bestV = v;
                    best = c;
                }
            }
        }
        if (best == null || bestV < least) {
            // the last points of their life, if it can aim at a player
            for (final Player p : ai.getOpponents()) {
                if (sa.canTarget(p) && n >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife()) {
                    sa.getTargets().add(p);
                    return yes();
                }
            }
            return no();
        }
        sa.getTargets().add(best);
        return yes();
    }

    // ---- Fire and Brimstone: damage to them and to itself, only for the win ----

    private static AiAbilityDecision lethalOnly(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return no();
        }
        final int n = forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDmg"), sa);
        sa.resetTargets();
        for (final Player p : ai.getOpponents()) {
            if (sa.canTarget(p) && n >= p.getLife() && p.canLoseLife() && !p.cantLoseForZeroOrLessLife() && ai.getLife() > n) {
                sa.getTargets().add(p);
                return yes();
            }
        }
        return no();
    }

    // ---- Command the Dreadhorde: creatures from any graveyard, a life for each point of mana value ----

    private static AiAbilityDecision lifeForGraveyard(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!sa.usesTargeting() || !ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        final java.util.List<Card> options = new java.util.ArrayList<>(CardUtil.getValidCardsToTarget(sa));
        // the best value for the life first
        options.sort(Comparator.comparingDouble((Card c) -> -(double) ComputerUtilCard.evaluateCreature(c) / Math.max(1, c.getCMC())));
        int life = ai.getLife() - 6;
        int v = 0;
        for (final Card c : options) {
            if (c.getCMC() <= life && ComputerUtilCard.evaluateCreature(c) > c.getCMC() * 25) {
                sa.getTargets().add(c);
                life -= c.getCMC();
                v += ComputerUtilCard.evaluateCreature(c) - c.getCMC() * 12;
            }
        }
        if (v < 200) {
            sa.resetTargets();
            return no();
        }
        return yes();
    }

    // ---- Mana Vapors: their lands don't untap next turn - when most of them are tapped now ----

    private static AiAbilityDecision landLock(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!sa.usesTargeting() || !ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        for (final Player p : ai.getOpponents()) {
            final long tapped = p.getLandsInPlay().stream().filter(Card::isTapped).count();
            if (sa.canTarget(p) && tapped >= 3 && tapped * 2 >= p.getLandsInPlay().size()) {
                sa.getTargets().add(p);
                return yes();
            }
        }
        return no();
    }

    // ---- Soul's Grace: life equal to a creature's power, when the life matters ----

    private static AiAbilityDecision lifeFromPower(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting() || !(forge.ai.ComputerUtil.aiLifeInDanger(ai, false, 0) || ai.getLife() <= 8)) {
            return no();
        }
        sa.resetTargets();
        Card best = null;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (best == null || c.getNetPower() > best.getNetPower()) {
                best = c;
            }
        }
        if (best == null || best.getNetPower() < 3) {
            return no();
        }
        sa.getTargets().add(best);
        return yes();
    }

    // ---- Phyrexian Purge: 3 life for each target - their creatures worth it, keeping 8 life back ----

    private static AiAbilityDecision lifePerTarget(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return no();
        }
        sa.resetTargets();
        final java.util.List<Card> theirs = new java.util.ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa),
                c -> c.getController().isOpponentOf(ai) && c.canBeDestroyed() && ComputerUtilCard.evaluateCreature(c) >= 150));
        theirs.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        int life = ai.getLife();
        for (final Card c : theirs) {
            if (life - 3 < 8) {
                break;
            }
            sa.getTargets().add(c);
            life -= 3;
        }
        return sa.getTargets().isEmpty() ? no() : yes();
    }

    // ---- verse counters ----

    private static AiAbilityDecision cashIn(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int n = host.getCounters(CounterType.getType("VERSE"));
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean danger = ComputerUtil.aiLifeInDanger(ai, false, 0);
        if (n < (danger ? 2 : 3) || !ai.getGame().getStack().isEmpty()) {
            return no();
        }
        if (!sa.usesTargeting()) {
            // Midsummer Revel: the Beasts, before attacking
            return ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1) || ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai) || danger
                    ? yes() : new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        final int max = Math.min(n, AbilityUtils.calculateAmount(host, sa.getParamOrDefault("TargetMax", "1"), sa));
        final List<Card> theirs = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai)));
        theirs.sort(Comparator.comparingInt((Card c) -> c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c)).reversed());
        if (theirs.size() < Math.min(3, max)) {
            return no();
        }
        theirs.stream().limit(max).forEach(c -> sa.getTargets().add(c));
        return yes();
    }
}
