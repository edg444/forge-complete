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
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

/**
 * The closing logics of the no-AI pass, one or two cards each.
 * <ul>
 * <li>AetherSnap: their tokens and counters outweigh its own. LockAttacker / LockBlocked (Ice Floe, Whip Vine): their
 * attacker, or the flier it blocked, held down. ResetCountersX (Anthroplasm): counters reset higher.</li>
 * <li>Veil (Autumn's Veil): their blue or black spell aimed at its creature or its spell. PumpSkip (Avizoa): +2/+2
 * worth a skipped untap, for lethal or a won fight. CarriageUntap (Black Carriage): a spare creature for the untap.</li>
 * <li>CounterTrick (Bounty of the Hunt): three counters on its creature that would die. StakeX (Burn at the Stake):
 * three damage per creature tapped, for lethal or their best creature. KeepOneEach (Cataclysm).</li>
 * <li>IdolBlock (Chimeric Idol): a 3/3 blocker. DangerLife (Dark Heart of the Wood, Claws of Gix): life when it's in
 * danger. DeadRingers. Disarm. StealAttacker (Disharmony). DrownX (Drown in Filth).</li>
 * <li>AkulCheat (Akul the Unrepentant): three spare creatures for the best creature in hand.</li>
 * <li>MatrixCounter (Life Matrix). PowerSweep (Nightmare Unmaking's modes). DelverReanimate (Phyrexian Delver's
 * trigger): the best creature its life affords. RallyUntap (Rally the Troops). SewersAttack (Sewers of Estark).
 * RandomInstant (Surreal Memoir). YareBlock (Yare).</li>
 * </ul>
 */
public final class EndgameAi {

    private EndgameAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "AetherSnap", "LockAttacker", "LockBlocked", "ResetCountersX", "Veil", "PumpSkip", "CarriageUntap", "CounterTrick",
                    "StakeX", "KeepOneEach", "IdolBlock", "DangerLife", "DeadRingers", "Disarm", "StealAttacker", "DrownX", "AkulCheat",
                    "MatrixCounter", "PowerSweep", "DelverReanimate", "RallyUntap", "SewersAttack", "RandomInstant", "YareBlock" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "AetherSnap" -> aetherSnap(ai, sa);
            case "LockAttacker" -> lockAttacker(ai, sa);
            case "LockBlocked" -> lockBlocked(ai, sa);
            case "ResetCountersX" -> resetCounters(ai, sa);
            case "Veil" -> veil(ai, sa);
            case "PumpSkip" -> pumpSkip(ai, sa);
            case "CarriageUntap" -> carriage(ai, sa);
            case "CounterTrick" -> counterTrick(ai, sa);
            case "StakeX" -> stake(ai, sa);
            case "KeepOneEach" -> keepOneEach(ai, sa);
            case "IdolBlock" -> idolBlock(ai, sa);
            case "DangerLife" -> ComputerUtil.aiLifeInDanger(ai, false, 0) ? YES : no(AiPlayDecision.CantPlayAi);
            case "DeadRingers" -> deadRingers(ai, sa);
            case "Disarm" -> disarm(ai, sa);
            case "StealAttacker" -> stealAttacker(ai, sa);
            case "DrownX" -> drown(ai, sa);
            case "AkulCheat" -> akul(ai, sa);
            case "MatrixCounter" -> matrix(ai, sa);
            case "PowerSweep" -> powerSweep(ai, sa);
            case "DelverReanimate" -> delver(ai, sa);
            case "RallyUntap" -> rally(ai, sa);
            case "SewersAttack" -> sewers(ai, sa);
            case "RandomInstant" -> ownMain(ai) && ai.getCardsIn(ZoneType.Graveyard).anyMatch(Card::isInstant) ? YES : no(AiPlayDecision.CantPlayAi);
            default -> yare(ai, sa);
        };
    }

    private static boolean ownMain(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)) && ai.getGame().getStack().isEmpty();
    }

    private static boolean theirAttack(final Player ai, final PhaseType step) {
        final Combat combat = ai.getGame().getCombat();
        return combat != null && !combat.getAttackingPlayer().equals(ai) && ai.getGame().getPhaseHandler().is(step);
    }

    private static boolean ownAttack(final Player ai, final PhaseType step) {
        final Combat combat = ai.getGame().getCombat();
        return combat != null && combat.getAttackingPlayer().equals(ai) && ai.getGame().getPhaseHandler().is(step);
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
    }

    private static AiAbilityDecision target(final SpellAbility sa, final Card c) {
        if (c == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(c);
        return YES;
    }

    private static AiAbilityDecision aetherSnap(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        int v = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            int w = c.isToken() ? worth(c) : 0;
            for (final forge.game.card.CounterType ct : c.getCounters().elementSet()) {
                if (!c.isToken()) {
                    w += (ct.is(CounterEnumType.M1M1) ? -25 : 25) * c.getCounters(ct);
                }
            }
            v += c.getController().isOpponentOf(ai) ? w : -w;
        }
        return v >= 200 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision lockAttacker(final Player ai, final SpellAbility sa) {
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        return target(sa, CardUtil.getValidCardsToTarget(sa).stream().max(Comparator.comparingInt(EndgameAi::worth)).orElse(null));
    }

    private static AiAbilityDecision lockBlocked(final Player ai, final SpellAbility sa) {
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        return target(sa, CardUtil.getValidCardsToTarget(sa).stream().max(Comparator.comparingInt(EndgameAi::worth)).orElse(null));
    }

    private static AiAbilityDecision resetCounters(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int x = forge.ai.ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        if (x < sa.getHostCard().getCounters(CounterEnumType.P1P1) + 2) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    private static AiAbilityDecision veil(final Player ai, final SpellAbility sa) {
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility s = si.getSpellAbility();
            final Card host = s.getHostCard();
            if (!s.isSpell() || !s.getActivatingPlayer().isOpponentOf(ai) || host == null
                    || !host.getColor().hasBlue() && !host.getColor().hasBlack()) {
                continue;
            }
            for (final GameObject o : s.getTargets()) {
                if (o instanceof Card c && c.getController().equals(ai) && c.isCreature()
                        || o instanceof SpellAbility t && t.getActivatingPlayer().equals(ai)) {
                    return YES;
                }
            }
        }
        return no(AiPlayDecision.AnotherTime);
    }

    private static AiAbilityDecision pumpSkip(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        final Card host = sa.getHostCard();
        if (combat == null || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !combat.isAttacking(host) && !combat.isBlocking(host)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        if (combat.isAttacking(host) && combat.getBlockers(host).isEmpty()) {
            final Player opp = combat.getDefenderPlayerByAttacker(host);
            final int need = opp == null ? Integer.MAX_VALUE : ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            return need > 0 && need <= 2 ? YES : no(AiPlayDecision.DoesntImpactCombat);
        }
        final List<Card> foes = combat.isAttacking(host) ? combat.getBlockers(host) : combat.getAttackersBlockedBy(host);
        int dmg = 0;
        for (final Card f : foes) {
            dmg += f.getNetCombatDamage();
        }
        final boolean saves = dmg >= host.getLethalDamage() && dmg < host.getLethalDamage() + 2;
        return saves ? YES : no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision carriage(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!host.isTapped()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final Card give = ComputerUtilCard.getWorstCreatureAI(CardLists.filter(ai.getCreaturesInPlay(),
                c -> !c.equals(host) && (c.isToken() || c.hasSVar("SacMe") || ComputerUtilCard.evaluateCreature(c) < 130)));
        if (give == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        SacrificePayoffAi.plan(host, give);
        return YES;
    }

    private static AiAbilityDecision counterTrick(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().equals(ai) || !combat.isAttacking(c) && !combat.isBlocking(c)) {
                continue;
            }
            int dmg = 0;
            for (final Card f : combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c)) {
                dmg += f.getNetCombatDamage();
            }
            if (dmg >= c.getLethalDamage() && dmg < c.getLethalDamage() + 3) {
                sa.resetTargets();
                sa.getTargets().add(c);
                sa.addDividedAllocation(c, 3);
                return YES;
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision stake(final Player ai, final SpellAbility sa) {
        final int idle = CardLists.count(ai.getCreaturesInPlay(), Card::isUntapped);
        final Player opp = ai.getWeakestOpponent();
        if (opp != null && sa.canTarget(opp) && opp.canLoseLife() && (opp.getLife() + 2) / 3 <= idle) {
            sa.setXManaCostPaid((opp.getLife() + 2) / 3);
            sa.resetTargets();
            sa.getTargets().add(opp);
            return YES;
        }
        final Card best = CardLists.filter(ai.getOpponents().getCreaturesInPlay(), c -> sa.canTarget(c) && (c.getLethalDamage() + 2) / 3 <= idle)
                .stream().max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 200 || !ownMain(ai)) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid((best.getLethalDamage() + 2) / 3);
        return target(sa, best);
    }

    // what a player loses keeping its best permanent of each type
    private static int keepOneLoss(final Player p) {
        int lost = 0;
        final List<Card> kept = new ArrayList<>();
        for (final String type : new String[] {"Artifact", "Creature", "Enchantment", "Land"}) {
            p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasStringType(type) && !kept.contains(c))
                    .max(Comparator.comparingInt(EndgameAi::worth)).ifPresent(kept::add);
        }
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (!kept.contains(c)) {
                lost += c.isLand() ? 60 : worth(c);
            }
        }
        return lost;
    }

    private static AiAbilityDecision keepOneEach(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        int theirs = 0;
        for (final Player opp : ai.getOpponents()) {
            theirs += keepOneLoss(opp);
        }
        return theirs - keepOneLoss(ai) >= 300 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision idolBlock(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS) || sa.getHostCard().isCreature()) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final boolean kills = combat.getAttackersOf(ai).stream().anyMatch(a -> a.getNetToughness() <= 3 && a.getNetCombatDamage() < 3
                && !a.hasKeyword(forge.game.keyword.Keyword.FLYING));
        return kills || ComputerUtilCombat.lifeInDanger(ai, combat) ? YES : no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision deadRingers(final Player ai, final SpellAbility sa) {
        final List<Card> theirs = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai));
        int best = 0;
        Card a = null;
        Card b = null;
        for (int i = 0; i < theirs.size(); i++) {
            for (int j = i + 1; j < theirs.size(); j++) {
                final Card x = theirs.get(i);
                final Card y = theirs.get(j);
                if (x.getColor().getColor() != y.getColor().getColor()) {
                    continue;
                }
                final int v = ComputerUtilCard.evaluateCreature(x) + ComputerUtilCard.evaluateCreature(y);
                if (v > best) {
                    best = v;
                    a = x;
                    b = y;
                }
            }
        }
        if (a == null || best < 250) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(a);
        sa.getTargets().add(b);
        return YES;
    }

    private static AiAbilityDecision disarm(final Player ai, final SpellAbility sa) {
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai)
                && c.getAttachedCards().anyMatch(Card::isEquipment)).stream()
                .max(Comparator.comparingInt(c -> (int) c.getAttachedCards().stream().filter(Card::isEquipment).count())).orElse(null);
        final Combat combat = ai.getGame().getCombat();
        final boolean timing = combat != null && ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) || ownMain(ai);
        return timing ? target(sa, best) : no(AiPlayDecision.AnotherTime);
    }

    private static AiAbilityDecision stealAttacker(final Player ai, final SpellAbility sa) {
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Card best = CardUtil.getValidCardsToTarget(sa).stream().max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        return best != null && ComputerUtilCard.evaluateCreature(best) >= 150 ? target(sa, best) : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision drown(final Player ai, final SpellAbility sa) {
        final SpellAbility pump = sa.getSubAbility();
        if (pump == null || !pump.usesTargeting()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        // the lands already there, and one more expected among the four milled
        final int x = CardLists.count(ai.getCardsIn(ZoneType.Graveyard), Card::isLand) + 1;
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(pump), c -> c.getController().isOpponentOf(ai) && c.getNetToughness() <= x)
                .stream().max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 130) {
            return no(AiPlayDecision.TargetingFailed);
        }
        pump.resetTargets();
        pump.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision akul(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Card host = sa.getHostCard();
        final List<Card> spare = new ArrayList<>(CardLists.filter(ai.getCreaturesInPlay(), c -> !c.equals(host)));
        spare.sort(Comparator.comparingInt(ComputerUtilCard::evaluateCreature));
        final Card big = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isCreature));
        if (spare.size() < 3 || big == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        // tokens and creatures marked to be sacrificed cost a third of what they'd rate
        final int given = spare.subList(0, 3).stream()
                .mapToInt(c -> c.isToken() || c.hasSVar("SacMe") ? ComputerUtilCard.evaluateCreature(c) / 3 : ComputerUtilCard.evaluateCreature(c)).sum();
        if (ComputerUtilCard.evaluateCreature(big) - given < 100) {
            return no(AiPlayDecision.CantPlayAi);
        }
        SacrificePayoffAi.plan(host, new forge.game.card.CardCollection(spare.subList(0, 3)));
        ZonePickAi.plan(host, List.of(big));
        return YES;
    }

    private static AiAbilityDecision matrix(final Player ai, final SpellAbility sa) {
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai)
                && c.getCounters(CounterEnumType.MATRIX) == 0).stream().max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        return best != null && ComputerUtilCard.evaluateCreature(best) >= 180 ? target(sa, best) : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision powerSweep(final Player ai, final SpellAbility sa) {
        final int hand = ai.getCardsIn(ZoneType.Hand).size() - (sa.getHostCard().isInZone(ZoneType.Hand) ? 1 : 0);
        final boolean greater = sa.getParamOrDefault("ChangeType", "").contains("powerGT");
        int v = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && (greater ? c.getNetPower() > hand : c.getNetPower() < hand)) {
                v += (c.getController().isOpponentOf(ai) ? 1 : -1) * ComputerUtilCard.evaluateCreature(c);
            }
        }
        return v >= 200 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision delver(final Player ai, final SpellAbility sa) {
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getOwner().equals(ai) && c.getCMC() < ai.getLife() - 5)
                .stream().max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (best == null) {
            // the trigger must target something; the cheapest costs least
            return target(sa, CardUtil.getValidCardsToTarget(sa).stream().min(Comparator.comparingInt(Card::getCMC)).orElse(null));
        }
        return target(sa, best);
    }

    private static AiAbilityDecision rally(final Player ai, final SpellAbility sa) {
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        return CardLists.count(ai.getCreaturesInPlay(), Card::isTapped) >= 2 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision sewers(final Player ai, final SpellAbility sa) {
        if (!ownAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Combat combat = ai.getGame().getCombat();
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> combat.isAttacking(c) && c.getNetCombatDamage() >= 3)
                .stream().max(Comparator.comparingInt(Card::getNetCombatDamage)).orElse(null);
        return target(sa, best);
    }

    private static AiAbilityDecision yare(final Player ai, final SpellAbility sa) {
        if (!theirAttack(ai, PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Combat combat = ai.getGame().getCombat();
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai) && c.isUntapped()
                && combat.getAttackersOf(ai).stream().anyMatch(a -> CombatUtil.canBlock(a, c) && c.getNetCombatDamage() + 3 >= a.getLethalDamage())).stream()
                .max(Comparator.comparingInt(Card::getNetToughness)).orElse(null);
        return target(sa, best);
    }
}
