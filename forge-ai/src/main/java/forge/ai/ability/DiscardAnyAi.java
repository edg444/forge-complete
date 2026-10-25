package forge.ai.ability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ DiscardAny (the Odyssey Rites): "discard any number of cards", then a payoff for each. The AI settles the
 * number when it decides and keeps to it as the spell resolves (the discard, or Rites of Initiation's number).
 * <ul>
 * <li>Sacred Rites: +0/+1 each, enough to save its creatures in a combat they'd die in.</li>
 * <li>Rites of Initiation: +1/+0 each (discarded at random), enough for lethal with the unblocked attackers.</li>
 * <li>Rites of Spring: a basic land for each, when it's short of lands and has cards to spare.</li>
 * <li>Rites of Refusal: {3} each, past what they could pay, to counter their spell.</li>
 * </ul>
 */
public final class DiscardAnyAi {

    private static final Map<Integer, Integer> PLANNED = new ConcurrentHashMap<>();

    private DiscardAnyAi() { }

    /** The number the AI decided on for this spell, if it decided on one. */
    public static Integer planned(final SpellAbility sa) {
        final SpellAbility root = sa.getRootAbility();
        if (!"DiscardAny".equals(root.getParam("AILogic"))) {
            return null;
        }
        return PLANNED.get(root.getHostCard().getId());
    }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static AiAbilityDecision yes(final SpellAbility sa, final int n) {
        PLANNED.put(sa.getHostCard().getId(), n);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // the cards it would discard anyway, worst first
    private static int spareCards(final Player ai, final Card host) {
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

    private static SpellAbility payoff(final SpellAbility sa) {
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.PumpAll || sub.getApi() == ApiType.ChangeZone || sub.getApi() == ApiType.Counter) {
                return sub;
            }
        }
        return null;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final SpellAbility pay = payoff(sa);
        if (pay == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        final int hand = ai.getCardsIn(ZoneType.Hand).size() - (host.isInZone(ZoneType.Hand) ? 1 : 0);
        if (hand <= 0) {
            return no(AiPlayDecision.CantPlayAi);
        }
        if (pay.getApi() == ApiType.PumpAll && pay.hasParam("NumDef")) {
            return toughness(ai, sa, spareCards(ai, host), hand);
        }
        if (pay.getApi() == ApiType.PumpAll) {
            return power(ai, sa, hand);
        }
        if (pay.getApi() == ApiType.ChangeZone) {
            return lands(ai, sa);
        }
        return counter(ai, sa, pay, spareCards(ai, host));
    }

    private static int fightDamage(final Card c, final Combat combat) {
        int dmg = 0;
        for (final Card o : combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c)) {
            dmg += o.getNetCombatDamage();
        }
        return dmg;
    }

    // Sacred Rites: the smallest number of cards that saves the most it can save, a card each worth a creature's half
    private static AiAbilityDecision toughness(final Player ai, final SpellAbility sa, final int spare, final int hand) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        int best = 0;
        int bestN = 0;
        for (int n = 1; n <= hand; n++) {
            int saved = 0;
            for (final Card c : ai.getCreaturesInPlay()) {
                if (!combat.isAttacking(c) && !combat.isBlocking(c) || !c.canBeDestroyed()) {
                    continue;
                }
                final int dmg = fightDamage(c, combat);
                final int left = c.getNetToughness() - c.getDamage();
                if (dmg >= left && dmg < left + n && !c.hasKeyword(forge.game.keyword.Keyword.INDESTRUCTIBLE)) {
                    saved += ComputerUtilCard.evaluateCreature(c);
                }
            }
            // past its spare cards, each one costs a real card
            final int v = saved - Math.max(0, n - spare) * 120 - n * 20;
            if (v > best) {
                best = v;
                bestN = n;
            }
        }
        return best >= 80 ? yes(sa, bestN) : no(AiPlayDecision.DoesntImpactCombat);
    }

    // Rites of Initiation: the random discards are any cards, so only for lethal
    private static AiAbilityDecision power(final Player ai, final SpellAbility sa, final int hand) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        if (combat == null || !ph.isPlayerTurn(ai) || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        for (final Player opp : ai.getOpponents()) {
            if (!opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
                continue;
            }
            final int unblocked = CardLists.filter(combat.getUnblockedAttackers(),
                    a -> opp.equals(combat.getDefenderPlayerByAttacker(a)) && !a.hasKeyword(forge.game.keyword.Keyword.INFECT)).size();
            final int need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            if (unblocked == 0 || need <= 0) {
                continue;
            }
            final int n = (need + unblocked - 1) / unblocked;
            if (n <= hand) {
                return yes(sa, n);
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    // Rites of Spring: the spells too big to cast soon, one each for the lands it's short of, two or more to be worth it
    private static AiAbilityDecision lands(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!(ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2)) && !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai))) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int lands = ai.getLandsInPlay().size() + CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isLand).size();
        final int stuck = CardLists.filter(ai.getCardsIn(ZoneType.Hand),
                c -> !c.isLand() && !c.equals(sa.getHostCard()) && c.getCMC() > lands + 1).size();
        final int basics = CardLists.filter(ai.getCardsIn(ZoneType.Library), Card::isBasicLand).size();
        final int n = Math.min(stuck, Math.min(basics, 6 - lands));
        return n >= 2 ? yes(sa, n) : no(AiPlayDecision.CantPlayAi);
    }

    // Rites of Refusal: enough {3}s that they can't pay, from cards it can spare
    private static AiAbilityDecision counter(final Player ai, final SpellAbility sa, final SpellAbility ctr, final int spare) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return no(AiPlayDecision.AnotherTime);
        }
        final SpellAbility top = game.getStack().peekAbility();
        final Player opp = top.getActivatingPlayer();
        if (!top.isSpell() || !opp.isOpponentOf(ai) || !top.isCounterableBy(ctr) || !ctr.canTargetSpellAbility(top)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int worth = top.getHostCard().isCreature() ? ComputerUtilCard.evaluateCreature(top.getHostCard())
                : 60 + top.getHostCard().getCMC() * 30;
        final int n = ComputerUtilMana.getAvailableManaEstimate(opp, false) / 3 + 1;
        // a big enough threat is worth a real card or two past the spare ones
        final int allowed = spare + (worth >= 200 ? 2 : worth >= 140 ? 1 : 0);
        final int hand = ai.getCardsIn(ZoneType.Hand).size() - 1;
        if (n > Math.min(allowed, hand) || worth < 100) {
            return no(AiPlayDecision.CantAfford);
        }
        ctr.resetTargets();
        ctr.getTargets().add(top);
        return yes(sa, n);
    }
}
