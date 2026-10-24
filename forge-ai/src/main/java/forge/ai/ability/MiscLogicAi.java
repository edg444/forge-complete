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
                || "WipeWithSub".equals(logic) || "RecycleForDamage".equals(logic) || "EchoDestroy".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "BounceBoth" -> bounceBoth(ai, sa);
            case "GraveShuffle" -> graveShuffle(ai, sa);
            case "RemoveCombatant" -> removeCombatant(ai, sa);
            case "WipeWithSub" -> wipeWithSub(ai, sa);
            case "RecycleForDamage" -> recycleForDamage(ai, sa);
            case "EchoDestroy" -> echoDestroy(ai, sa);
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
