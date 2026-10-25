package forge.ai.ability;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ Sweep (Barrel Down Sokenzan, Charge Across the Araba): "return any number of lands of a type" for an effect
 * per land, so the AI settles the number when it decides - just enough, tapped lands first - and returns that many as
 * the spell resolves.
 * <ul>
 * <li>Barrel Down Sokenzan: 2 damage a Mountain, on their best creature that many kill, when it's worth the lands.</li>
 * <li>Charge Across the Araba: +1/+1 a Plains to its creatures, once blocks are known, for lethal unblocked.</li>
 * </ul>
 */
public final class SweepAi {

    private static final Map<Integer, Integer> LEFT = new ConcurrentHashMap<>();

    private SweepAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static List<Card> lands(final Player ai, final SpellAbility sa) {
        final String type = sa.getParamOrDefault("ChangeType", "Land.YouCtrl");
        final List<Card> lands = CardLists.getValidCards(ai.getCardsIn(forge.game.zone.ZoneType.Battlefield), type.split(","), ai, sa.getHostCard(), sa);
        // the tapped ones cost nothing more this turn
        lands.sort((a, b) -> Boolean.compare(b.isTapped(), a.isTapped()));
        return lands;
    }

    /** As the spell resolves: the next land to return, or null once it has returned what it planned. */
    public static Card nextLand(final Player ai, final SpellAbility sa, final CardCollectionView options) {
        if (!"Sweep".equals(sa.getRootAbility().getParam("AILogic"))) {
            return null;
        }
        final int id = sa.getHostCard().getId();
        final int left = LEFT.getOrDefault(id, 0);
        if (left <= 0) {
            return null;
        }
        for (final Card c : lands(ai, sa)) {
            if (options.contains(c)) {
                LEFT.put(id, left - 1);
                return c;
            }
        }
        return null;
    }

    public static boolean planned(final SpellAbility sa) {
        return "Sweep".equals(sa.getRootAbility().getParam("AILogic"));
    }

    // what returning n of them costs: an untapped land is mana it loses now
    private static int cost(final List<Card> lands, final int n) {
        int c = 0;
        for (int i = 0; i < n; i++) {
            c += lands.get(i).isTapped() ? 20 : 45;
        }
        return c;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final List<Card> lands = lands(ai, sa);
        final SpellAbility pay = sa.getSubAbility();
        if (lands.isEmpty() || pay == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        if (pay.getApi() == ApiType.DealDamage) {
            Card best = null;
            int bestN = 0;
            int bestValue = 0;
            for (final Card c : CardUtil.getValidCardsToTarget(pay)) {
                if (!c.getController().isOpponentOf(ai) || !c.canBeDestroyed()) {
                    continue;
                }
                final int n = (c.getLethalDamage() + 1) / 2;
                if (n > lands.size()) {
                    continue;
                }
                final int v = ComputerUtilCard.evaluateCreature(c) - cost(lands, n);
                if (v > bestValue) {
                    bestValue = v;
                    best = c;
                    bestN = n;
                }
            }
            if (best == null || bestValue < 80) {
                return no(AiPlayDecision.TargetingFailed);
            }
            pay.resetTargets();
            pay.getTargets().add(best);
            LEFT.put(sa.getHostCard().getId(), bestN);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (pay.getApi() == ApiType.Pump && pay.usesTargeting()) {
            // Plow Through Reito: +1/+1 a Plains on its unblocked attacker, for lethal
            final Combat combat = ai.getGame().getCombat();
            if (combat == null || !combat.getAttackingPlayer().equals(ai) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            for (final Card a : combat.getUnblockedAttackers()) {
                final Player opp = combat.getDefenderPlayerByAttacker(a);
                final int need = opp == null ? Integer.MAX_VALUE : ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
                if (pay.canTarget(a) && need > 0 && need <= lands.size()) {
                    pay.resetTargets();
                    pay.getTargets().add(a);
                    LEFT.put(sa.getHostCard().getId(), need);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        if (pay.getApi() == ApiType.Discard && pay.usesTargeting()) {
            // Sink into Takenuma: a discard a Swamp, their hand, from its tapped Swamps
            final Player opp = ai.getStrongestOpponent();
            final int tapped = (int) lands.stream().filter(Card::isTapped).count();
            final int n = opp == null ? 0 : Math.min(opp.getCardsIn(forge.game.zone.ZoneType.Hand).size(), Math.max(tapped, 2));
            if (n < 2 || !pay.canTarget(opp)) {
                return no(AiPlayDecision.CantPlayAi);
            }
            pay.resetTargets();
            pay.getTargets().add(opp);
            LEFT.put(sa.getHostCard().getId(), Math.min(n, lands.size()));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (pay.getApi() == ApiType.PumpAll) {
            final Combat combat = ai.getGame().getCombat();
            if (combat == null || !combat.getAttackingPlayer().equals(ai) || !ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            for (final Player opp : ai.getOpponents()) {
                final int unblocked = CardLists.count(combat.getUnblockedAttackers(), a -> opp.equals(combat.getDefenderPlayerByAttacker(a)));
                final int need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
                if (unblocked == 0 || need <= 0 || !opp.canLoseLife()) {
                    continue;
                }
                final int n = (need + unblocked - 1) / unblocked;
                if (n <= lands.size()) {
                    LEFT.put(sa.getHostCard().getId(), n);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        return no(AiPlayDecision.CantPlayAi);
    }
}
