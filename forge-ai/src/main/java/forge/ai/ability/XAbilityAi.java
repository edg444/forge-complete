package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Cards whose X is the whole decision, each worked out for what that X does.
 * <ul>
 * <li>AlexiX (Alexi, Zephyr Mage): their creatures most worth bouncing, when the two discards are spare cards.</li>
 * <li>NoCombatX (Droning Bureaucrats): the mana value whose creatures of theirs it most wants out of the combat ahead.</li>
 * <li>GraveShrink (Painbringer): their best creature, with just enough graveyard cards exiled to kill it.</li>
 * <li>LiftX (Mercadian Lift): its best creature in hand whose mana value the winch counters cover.</li>
 * <li>StealCountersX (Simic Manipulator): their best creature whose power its counters cover.</li>
 * <li>TutorCounters (Night Dealings): the priciest card in its library the theft counters cover, at their end step.</li>
 * <li>DiscardBurn (Firestorm): the X targets that kill the most for X spare discards.</li>
 * <li>DoubleX (Exponential Growth): an unblocked attacker doubled just enough for lethal.</li>
 * <li>AttackCount (Foul-Tongue Shriek): during its attack, two or more attackers.</li>
 * <li>DiedCount (Fresh Meat): two or more of its creatures died this turn.</li>
 * <li>ArtifactCounters (Lifecraft Awakening): X counters on its least valuable artifact, three or more.</li>
 * <li>SpoilsX (Spoils of War): all the counters on its best creature, two or more.</li>
 * </ul>
 */
public final class XAbilityAi {

    private XAbilityAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "AlexiX", "NoCombatX", "GraveShrink", "LiftX", "StealCountersX", "TutorCounters", "DiscardBurn", "DoubleX",
                    "AttackCount", "DiedCount", "ArtifactCounters", "SpoilsX", "CoppercoatsX", "RallyX", "LockX", "GraveTokenX",
                    "BaseX", "Elephantize", "SwampWalls", "AttackTaxX" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "AlexiX" -> alexi(ai, sa);
            case "NoCombatX" -> noCombat(ai, sa);
            case "GraveShrink" -> graveShrink(ai, sa);
            case "LiftX" -> lift(ai, sa);
            case "StealCountersX" -> stealCounters(ai, sa);
            case "TutorCounters" -> tutorCounters(ai, sa);
            case "DiscardBurn" -> discardBurn(ai, sa);
            case "DoubleX" -> doubleX(ai, sa);
            case "AttackCount" -> attackCount(ai, sa);
            case "DiedCount" -> diedCount(ai, sa);
            case "ArtifactCounters" -> artifactCounters(ai, sa);
            case "CoppercoatsX" -> coppercoats(ai, sa);
            case "RallyX" -> rally(ai, sa);
            case "LockX" -> lock(ai, sa);
            case "GraveTokenX" -> graveToken(ai, sa);
            case "BaseX" -> baseX(ai, sa);
            case "Elephantize" -> elephantize(ai, sa);
            case "SwampWalls" -> swampWalls(ai, sa);
            case "AttackTaxX" -> attackTax(ai, sa);
            default -> spoils(ai, sa);
        };
    }

    // the cards it would discard anyway
    private static int spare(final Player ai, final Card host) {
        final CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        hand.remove(host);
        int n = 0;
        for (Card c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand); c != null;
                c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand)) {
            hand.remove(c);
            n++;
        }
        return n;
    }

    private static boolean quiet(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1) || ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static AiAbilityDecision alexi(final Player ai, final SpellAbility sa) {
        if (!quiet(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int mana = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        final List<Card> theirs = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai)));
        theirs.sort(Comparator.comparingInt((Card c) -> bounceValue(c)).reversed());
        final int n = Math.min(mana, theirs.size());
        int value = 0;
        for (int i = 0; i < n; i++) {
            value += bounceValue(theirs.get(i));
        }
        // two real cards are worth about 240
        final int discardCost = spare(ai, sa.getHostCard()) >= 2 ? 40 : 240;
        if (n == 0 || value - discardCost < 100) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(n);
        sa.resetTargets();
        theirs.subList(0, n).forEach(c -> sa.getTargets().add(c));
        return YES;
    }

    private static int bounceValue(final Card c) {
        return c.isToken() ? ComputerUtilCard.evaluateCreature(c) : ComputerUtilCard.evaluateCreature(c) / 3 + c.getCMC() * 20
                + c.getAttachedCards().size() * 60;
    }

    private static AiAbilityDecision noCombat(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int mana = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        final boolean theirTurn = !ph.isPlayerTurn(ai);
        int best = 0;
        int bestX = -1;
        for (int x = 0; x <= mana; x++) {
            int v = 0;
            for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                if (!c.isCreature() || c.getCMC() != x || c.isTapped()) {
                    continue;
                }
                // their attackers on their turn, their blockers on its own; its own creatures with that value sit out too
                v += (c.getController().isOpponentOf(ai) ? 1 : -1) * (theirTurn ? c.getNetCombatDamage() * 2 : ComputerUtilCard.evaluateCreature(c) / 40);
            }
            if (v > best) {
                best = v;
                bestX = x;
            }
        }
        if (bestX < 0 || best < 4) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestX);
        return YES;
    }

    private static AiAbilityDecision graveShrink(final Player ai, final SpellAbility sa) {
        final int grave = ai.getCardsIn(ZoneType.Graveyard).size();
        Card best = null;
        int bestX = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            final int x = c.getNetToughness();
            if (c.getController().isOpponentOf(ai) && x <= grave && x > 0
                    && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
                bestX = x;
            }
        }
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 120) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        sa.setXManaCostPaid(bestX);
        return YES;
    }

    private static int counters(final Card host, final SpellAbility sa) {
        for (final forge.game.cost.CostPart p : sa.getPayCosts().getCostParts()) {
            if (p instanceof forge.game.cost.CostRemoveCounter rc) {
                final CounterType ct = rc.counter;
                return host.getCounters(ct);
            }
        }
        return 0;
    }

    private static AiAbilityDecision lift(final Player ai, final SpellAbility sa) {
        final int n = counters(sa.getHostCard(), sa);
        final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCardsIn(ZoneType.Hand),
                c -> c.isCreature() && c.getCMC() <= n && c.getCMC() > 0));
        if (best == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(best.getCMC());
        return YES;
    }

    private static AiAbilityDecision stealCounters(final Player ai, final SpellAbility sa) {
        final int n = counters(sa.getHostCard(), sa);
        Card best = null;
        for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
            final int x = Math.max(1, c.getNetPower());
            sa.setXManaCostPaid(x);
            if (x <= n && sa.canTarget(c) && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
            }
        }
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 130) {
            sa.setXManaCostPaid(null);
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.setXManaCostPaid(Math.max(1, best.getNetPower()));
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision tutorCounters(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int n = counters(sa.getHostCard(), sa);
        int best = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (!c.isLand() && c.getCMC() <= n) {
                best = Math.max(best, c.getCMC());
            }
        }
        if (best < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(best);
        return YES;
    }

    private static AiAbilityDecision discardBurn(final Player ai, final SpellAbility sa) {
        final int spare = spare(ai, sa.getHostCard());
        final int hand = ai.getCardsIn(ZoneType.Hand).size() - 1;
        int best = 0;
        List<forge.game.GameEntity> bestPicks = null;
        for (int x = 1; x <= hand; x++) {
            final int k = x;
            final List<Card> kills = new ArrayList<>(CardLists.filter(ai.getOpponents().getCreaturesInPlay(),
                    c -> k >= c.getLethalDamage() && c.canBeDestroyed() && sa.canTarget(c)));
            kills.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
            final List<forge.game.GameEntity> picks = new ArrayList<>(kills.subList(0, Math.min(x, kills.size())));
            int v = picks.stream().mapToInt(e -> ComputerUtilCard.evaluateCreature((Card) e)).sum();
            final Player opp = ai.getWeakestOpponent();
            if (picks.size() < x && opp != null && sa.canTarget(opp)) {
                picks.add(opp);
                v += x >= opp.getLife() && opp.canLoseLife() ? 1000 : x * 10;
            }
            if (picks.size() < x) {
                continue;
            }
            v -= Math.max(0, x - spare) * 120 + x * 20;
            if (v > best) {
                best = v;
                bestPicks = picks;
            }
        }
        if (bestPicks == null || best < 120) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        bestPicks.forEach(e -> sa.getTargets().add(e));
        return YES;
    }

    private static AiAbilityDecision doubleX(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        sa.setXManaCostPaid(null);
        // {X}{X}{R}{R}: what's left after the fixed part, per X
        final forge.card.mana.ManaCost cost = sa.getPayCosts().getTotalMana();
        final int mana = (ComputerUtilMana.getAvailableManaEstimate(ai, false) - cost.getCMC()) / Math.max(1, cost.countX());
        for (final Card a : combat.getUnblockedAttackers()) {
            final Player opp = combat.getDefenderPlayerByAttacker(a);
            if (opp == null || !sa.canTarget(a) || a.getNetPower() <= 0) {
                continue;
            }
            final int need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            int power = a.getNetPower();
            for (int x = 1; x <= mana; x++) {
                power *= 2;
                if (power - a.getNetPower() >= need) {
                    sa.setXManaCostPaid(x);
                    sa.resetTargets();
                    sa.getTargets().add(a);
                    return YES;
                }
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision attackCount(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final int n = combat.getAttackers().size();
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !sa.canTarget(opp) || n < 2 && n < opp.getLife()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }

    private static AiAbilityDecision diedCount(final Player ai, final SpellAbility sa) {
        final int n = forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(), "X", sa);
        return n >= 2 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision artifactCounters(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1) && !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final forge.card.mana.ManaCost cost = sa.getPayCosts().getTotalMana();
        final int x = (ComputerUtilMana.getAvailableManaEstimate(ai, false) - cost.getCMC()) / Math.max(1, cost.countX());
        final Card art = ComputerUtilCard.getWorstAI(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai)));
        if (art == null || x < 3) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.setXManaCostPaid(x);
        sa.resetTargets();
        sa.getTargets().add(art);
        return YES;
    }

    private static AiAbilityDecision spoils(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        int x = 0;
        for (final Player opp : ai.getOpponents()) {
            x = Math.max(x, CardLists.count(opp.getCardsIn(ZoneType.Graveyard), c -> c.isCreature() || c.isArtifact()));
        }
        final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCreaturesInPlay(), sa::canTarget));
        if (best == null || x < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        sa.addDividedAllocation(best, x);
        return YES;
    }

    // ---- group C ----

    private static int xMana(final Player ai, final SpellAbility sa) {
        final forge.card.mana.ManaCost cost = sa.getPayCosts() == null ? null : sa.getPayCosts().getTotalMana();
        if (cost == null) {
            return ComputerUtilMana.getAvailableManaEstimate(ai, false);
        }
        return Math.max(0, (ComputerUtilMana.getAvailableManaEstimate(ai, false) - cost.getCMC()) / Math.max(1, cost.countX()));
    }

    // Call the Coppercoats: a token for each of their creatures, at their end step or as their attack comes in
    private static AiAbilityDecision coppercoats(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.isPlayerTurn(ai) || !ph.is(PhaseType.END_OF_TURN) && !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        if (opp == null || !sa.canTarget(opp) || opp.getCreaturesInPlay().size() < 3) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }

    // Rally the Ancestors: its graveyard's creatures as blockers against their attack (they leave at its upkeep)
    private static AiAbilityDecision rally(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Combat combat = ai.getGame().getCombat();
        if (ph.isPlayerTurn(ai) || combat == null || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final int x = xMana(ai, sa);
        final int worth = CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), c -> c.isCreature() && c.getCMC() <= x)
                .stream().mapToInt(ComputerUtilCard::evaluateCreature).sum();
        if (worth < 300 && !(worth > 0 && ComputerUtilCombat.lifeInDanger(ai, combat))) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    // Entrancing Lyre: their best creature it can lock down, before its own attack or theirs
    private static AiAbilityDecision lock(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) || ph.is(PhaseType.UNTAP)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int mana = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        Card best = null;
        for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
            final int x = Math.max(0, c.getNetPower());
            if (x > mana || c.isTapped()) {
                continue;
            }
            sa.setXManaCostPaid(x);
            if (sa.canTarget(c) && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
            }
        }
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 150) {
            sa.setXManaCostPaid(null);
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.setXManaCostPaid(Math.max(0, best.getNetPower()));
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    // Corpseweft: all its graveyard creatures for one big Horror, two or more, at their end step
    private static AiAbilityDecision graveToken(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int n = CardLists.count(ai.getCardsIn(ZoneType.Graveyard), Card::isCreature);
        if (n < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(n);
        return YES;
    }

    // Biomass Mutation: once blocks are known, the least base X/X that makes its unblocked attackers lethal
    private static AiAbilityDecision baseX(final Player ai, final SpellAbility sa) {
        final Combat combat = ai.getGame().getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final int mana = xMana(ai, sa);
        for (final Player opp : ai.getOpponents()) {
            final List<Card> open = CardLists.filter(combat.getUnblockedAttackers(), a -> opp.equals(combat.getDefenderPlayerByAttacker(a)));
            if (open.isEmpty() || !opp.canLoseLife()) {
                continue;
            }
            for (int x = 1; x <= mana; x++) {
                final int each = x;
                if (open.stream().mapToInt(a -> Math.max(each, a.getNetPower())).sum() >= opp.getLife()) {
                    sa.setXManaCostPaid(x);
                    return YES;
                }
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    // Gift of Tusks: their creature that's worth much more than a vanilla 3/3
    private static AiAbilityDecision elephantize(final Player ai, final SpellAbility sa) {
        final Card elephant = CardUtil.getValidCardsToTarget(sa).stream().filter(c -> c.getController().isOpponentOf(ai))
                .max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        // a vanilla 3/3 rates about 155, so only something worth clearly more
        if (elephant == null || ComputerUtilCard.evaluateCreature(elephant) < 230) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(elephant);
        return YES;
    }

    // Fendeep Summoner: two 3/5 Swamps to block their attack
    private static AiAbilityDecision swampWalls(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Combat combat = ai.getGame().getCombat();
        if (ph.isPlayerTurn(ai) || combat == null || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final boolean worth = combat.getAttackersOf(ai).stream().anyMatch(a -> a.getNetCombatDamage() < 5
                && !a.hasKeyword(forge.game.keyword.Keyword.FLYING)) || ComputerUtilCombat.lifeInDanger(ai, combat);
        final List<Card> swamps = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai) && c.isUntapped());
        if (!worth || swamps.isEmpty()) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.resetTargets();
        swamps.stream().limit(2).forEach(c -> sa.getTargets().add(c));
        return YES;
    }

    // War Tax: before their attack, {X} an attacker when they don't have the mana for all of theirs
    private static AiAbilityDecision attackTax(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.isPlayerTurn(ai) || !ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int x = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        final Player opp = (Player) ph.getPlayerTurn();
        final int attackers = CardLists.count(opp.getCreaturesInPlay(), c -> c.isUntapped() && !c.hasSickness());
        final int power = opp.getCreaturesInPlay().stream().filter(c -> c.isUntapped() && !c.hasSickness()).mapToInt(Card::getNetCombatDamage).sum();
        if (x < 2 || attackers == 0 || power < 4 || ComputerUtilMana.getAvailableManaEstimate(opp, false) >= x * attackers) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }
}
