package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spells whose X is a mana value limit, so X is picked for the payoff rather than as high as it goes.
 * <ul>
 * <li>AILogic$ StealX (Dominate, Entrancing Melody): X is the mana value of the best creature of theirs it can afford.</li>
 * <li>AILogic$ WipeX (Forced March, Gaze of Granite): every affordable X is tried for what the sweep destroys on each
 * side, the lowest X giving the best result.</li>
 * </ul>
 */
public final class XChoiceAi {

    private XChoiceAi() { }

    public static boolean handles(final String logic) {
        return "StealX".equals(logic) || "WipeX".equals(logic) || "DestroyX".equals(logic) || "GraveX".equals(logic)
                || "TapX".equals(logic);
    }

    private static int maxX(final Player ai, final SpellAbility sa) {
        // Toxic Deluge: X is paid in life, keeping some back
        if (sa.getPayCosts() != null && sa.getPayCosts().getCostParts().stream()
                .anyMatch(p -> p instanceof forge.game.cost.CostPayLife && "X".equals(p.getAmount()))) {
            return ai.getLife() - 5;
        }
        if (sa.getPayCosts() == null || sa.getPayCosts().getTotalMana() == null) {
            return ComputerUtilMana.getAvailableManaEstimate(ai, false);
        }
        final forge.card.mana.ManaCost mana = sa.getPayCosts().getTotalMana();
        // Orcish Settlers pays {X}{X}
        return (ComputerUtilMana.getAvailableManaEstimate(ai, false) - mana.getCMC()) / Math.max(1, mana.countX());
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
    }

    // what one graveyard card is worth to the effect taking it
    private static int graveValue(final Player ai, final SpellAbility take, final Card c) {
        final String dest = take.getParamOrDefault("Destination", "");
        final SpellAbility next = take.getSubAbility();
        if ("Exile".equals(dest) && next != null && next.getApi() == forge.game.ability.ApiType.CopyPermanent) {
            // Hour of Eternity: a 4/4 copy
            return c.isCreature() ? 170 : 0;
        }
        if ("Exile".equals(dest) && next != null && next.getApi() == forge.game.ability.ApiType.Token) {
            // Midnight Ritual: a 2/2 for each
            return 80;
        }
        if ("Battlefield".equals(dest)) {
            final int v = worth(c);
            // Wake the Dead, Postmortem Lunge: only for the turn
            return take.hasParam("AtEOT") || next != null && next.hasParam("AtEOT") ? v / 3 : v;
        }
        if ("Hand".equals(dest)) {
            return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) / 2 : 30 + c.getCMC() * 20;
        }
        return 0;
    }

    // Shattered Crypt, Midnight Ritual, Hour of Eternity, Divergent Equation, Dance of the Manse, Wake the Dead,
    // Postmortem Lunge: each X is tried for the best cards it lets the spell take, less the mana
    private static AiAbilityDecision graveX(final Player ai, final SpellAbility sa, final int max) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        final SpellAbility take = findTake(sa);
        if (take == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final boolean battlefieldForTheTurn = "Battlefield".equals(take.getParam("Destination")) && graveTemporary(take);
        if (battlefieldForTheTurn && sa.getHostCard().isInstant()) {
            // Wake the Dead: blockers, once they attack
            if (ph.isPlayerTurn(ai) || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
        } else if (battlefieldForTheTurn && (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1))) {
            // Postmortem Lunge: a hasty attacker
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        // Shattered Crypt: X life too
        final boolean losesLife = take.getSubAbility() != null && take.getSubAbility().getApi() == forge.game.ability.ApiType.LoseLife;
        int best = 0;
        int bestX = -1;
        java.util.List<Card> bestPicks = null;
        for (int x = 1; x <= max; x++) {
            sa.setXManaCostPaid(x);
            take.resetTargets();
            final int n = x;
            final java.util.List<Card> cands = CardLists.filter(forge.game.card.CardUtil.getValidCardsToTarget(take), c -> c.getOwner().equals(ai));
            cands.sort(java.util.Comparator.comparingInt((Card c) -> graveValue(ai, take, c)).reversed());
            final int limit = Math.min(n, take.getMaxTargets());
            if (take.getMinTargets() > cands.size()) {
                continue;
            }
            final java.util.List<Card> picks = new java.util.ArrayList<>(cands.subList(0, Math.min(limit, cands.size())));
            int v = picks.stream().mapToInt(c -> graveValue(ai, take, c)).sum() - x * 15;
            if (losesLife) {
                v -= x * (forge.ai.ComputerUtil.aiLifeInDanger(ai, false, x) ? 50 : 6);
            }
            if (v > best) {
                best = v;
                bestX = x;
                bestPicks = picks;
            }
        }
        if (bestX < 0 || best < 80) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestX);
        take.resetTargets();
        bestPicks.forEach(c -> take.getTargets().add(c));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static SpellAbility findTake(final SpellAbility sa) {
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s.usesTargeting() && s.getApi() == forge.game.ability.ApiType.ChangeZone && s.getParamOrDefault("Origin", "").contains("Graveyard")) {
                return s;
            }
        }
        return null;
    }

    // Malicious Advice, Mishra's Helix, Synod Artificer: tap X of theirs - just the blockers that keep its attack from
    // being lethal, or else their lands as their turn starts
    private static AiAbilityDecision tapX(final Player ai, final SpellAbility sa, final int maxMana) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !sa.usesTargeting() || maxMana < 1) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // Malicious Advice: X life too
        final boolean lifeToo = sa.getSubAbility() != null && sa.getSubAbility().getApi() == forge.game.ability.ApiType.LoseLife;
        final int max = lifeToo ? Math.min(maxMana, ai.getLife() - 5) : maxMana;
        sa.setXManaCostPaid(Math.max(1, max));
        sa.resetTargets();
        final java.util.List<Card> options = CardLists.filter(forge.game.card.CardUtil.getValidCardsToTarget(sa),
                c -> c.getController().equals(opp) && c.isUntapped());
        final java.util.List<Card> picks = new java.util.ArrayList<>();
        if (ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1) && opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife()) {
            final java.util.List<Card> attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> forge.game.combat.CombatUtil.canAttack(c, opp));
            final java.util.List<Card> blockers = new java.util.ArrayList<>(CardLists.filter(opp.getCreaturesInPlay(), forge.game.combat.CombatUtil::canBlock));
            blockers.sort(java.util.Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
            for (final Card b : blockers) {
                if (ForcedCombatAi.assault(attackers, blockers.stream().filter(x -> !picks.contains(x)).toList(), opp) >= opp.getLife()) {
                    break;
                }
                if (options.contains(b) && picks.size() < max) {
                    picks.add(b);
                }
            }
            final java.util.List<Card> left = blockers.stream().filter(x -> !picks.contains(x)).toList();
            if (picks.isEmpty() || ForcedCombatAi.assault(attackers, left, opp) < opp.getLife()) {
                picks.clear();
            }
        }
        if (picks.isEmpty() && !lifeToo && ph.isPlayerTurn(opp) && ph.is(PhaseType.UPKEEP)) {
            // their mana for the turn
            options.stream().filter(Card::isLand).limit(max).forEach(picks::add);
            if (picks.size() < 2) {
                picks.clear();
            }
        }
        if (picks.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(picks.size());
        sa.resetTargets();
        picks.forEach(c -> sa.getTargets().add(c));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean graveTemporary(final SpellAbility take) {
        for (SpellAbility s = take; s != null; s = s.getSubAbility()) {
            if (s.hasParam("AtEOT")) {
                return true;
            }
        }
        return false;
    }

    // Suffer the Past: X cards of their graveyard, a point drained for each
    private static AiAbilityDecision drainGraveyard(final Player ai, final SpellAbility sa, final int max) {
        final SpellAbility cards = sa.getSubAbility();
        final Player opp = ai.getWeakestOpponent();
        if (cards == null || opp == null || !sa.canTarget(opp)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final int x = Math.min(max, opp.getCardsIn(ZoneType.Graveyard).size());
        final boolean lethal = x >= opp.getLife() && opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife();
        if (x < 3 && !lethal) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        sa.resetTargets();
        sa.getTargets().add(opp);
        cards.resetTargets();
        opp.getCardsIn(ZoneType.Graveyard).stream().limit(x).forEach(c -> cards.getTargets().add(c));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final int max = maxX(ai, sa);
        if (max < 0) {
            return new AiAbilityDecision(0, AiPlayDecision.CantAfford);
        }
        if ("TapX".equals(sa.getParam("AILogic"))) {
            return tapX(ai, sa, max);
        }
        if ("GraveX".equals(sa.getParam("AILogic"))) {
            return sa.getApi() == forge.game.ability.ApiType.Pump ? drainGraveyard(ai, sa, max) : graveX(ai, sa, max);
        }
        int best = Integer.MIN_VALUE;
        int bestX = -1;
        Card bestTarget = null;
        if ("DestroyX".equals(sa.getParam("AILogic"))) {
            // Dregs of Sorrow, Orcish Settlers: as many of their worthwhile creatures (or lands) as it can pay for
            sa.setXManaCostPaid(Math.max(1, max));
            sa.resetTargets();
            final java.util.List<Card> picks = CardLists.filter(forge.game.card.CardUtil.getValidCardsToTarget(sa),
                    c -> c.getController().isOpponentOf(ai) && c.canBeDestroyed() && worth(c) >= 100);
            picks.sort(java.util.Comparator.comparingInt(XChoiceAi::worth).reversed());
            final int x = Math.min(max, picks.size());
            // a creature that gives itself up for it wants more than one
            final int least = sa.getHostCard().isCreature() && forge.ai.ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts()) ? 2 : 1;
            if (x < least) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.setXManaCostPaid(x);
            sa.resetTargets();
            picks.stream().limit(x).forEach(c -> sa.getTargets().add(c));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if ("StealX".equals(sa.getParam("AILogic"))) {
            for (final Card c : CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), c -> c.isCreature() && c.getController().isOpponentOf(ai))) {
                if (c.getCMC() > max) {
                    continue;
                }
                sa.setXManaCostPaid(c.getCMC());
                sa.resetTargets();
                if (!sa.canTarget(c)) {
                    continue;
                }
                final int v = ComputerUtilCard.evaluateCreature(c);
                if (v > best) {
                    best = v;
                    bestX = c.getCMC();
                    bestTarget = c;
                }
            }
            if (bestTarget == null || best < 100) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.setXManaCostPaid(bestX);
            sa.resetTargets();
            sa.getTargets().add(bestTarget);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // Black Sun's Zenith: -1/-1 counters (or -X/-X) kill what has toughness X or less, indestructible or not
        final boolean shrinks = sa.getApi() == forge.game.ability.ApiType.PutCounterAll || sa.getApi() == forge.game.ability.ApiType.PumpAll;
        for (int x = shrinks ? 1 : 0; x <= max; x++) {
            sa.setXManaCostPaid(x);
            int v = 0;
            for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("ValidCards"), ai, sa.getHostCard(), sa)) {
                if (shrinks ? c.getNetToughness() > x : !c.canBeDestroyed()) {
                    continue;
                }
                final int w = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                v += c.getController().isOpponentOf(ai) ? w : c.getController().equals(ai) ? -w : 0;
            }
            if (v > best) {
                best = v;
                bestX = x;
            }
        }
        if (bestX < 0 || best < 150) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestX);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
