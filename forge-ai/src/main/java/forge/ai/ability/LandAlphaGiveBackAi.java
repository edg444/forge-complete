package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * <ul>
 * <li>AILogic$ LandAlpha (Natural Affinity, Sylvan Awakening): lands become 2/2 creatures, cast in its first main
 * phase only when the attack is lethal with their biggest attackers blocked (their lands too, when all lands
 * animate).</li>
 * <li>AILogic$ GiveBack (Nullmage Advocate, Pulsemage Advocate): cards from their graveyard go back to their hand as
 * the price, their least useful ones, when what follows is worth more than what they get back.</li>
 * </ul>
 */
public final class LandAlphaGiveBackAi {

    private LandAlphaGiveBackAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    public static boolean handles(final String logic) {
        return "LandAlpha".equals(logic) || "GiveBack".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return "LandAlpha".equals(sa.getParam("AILogic")) ? landAlpha(ai, sa) : giveBack(ai, sa);
    }

    private static AiAbilityDecision landAlpha(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final boolean everyones = !sa.getParamOrDefault("ValidCards", "").contains("YouCtrl");
        final boolean haste = sa.getParamOrDefault("Keywords", "").contains("Haste");
        // Rude Awakening: a mode, so the spell's own cost
        final SpellAbility root = sa.getRootAbility();
        final int cost = root.getPayCosts() == null || root.getPayCosts().getTotalMana() == null ? 0 : root.getPayCosts().getTotalMana().getCMC();
        final List<Card> lands = CardLists.filter(ai.getLandsInPlay(), c -> c.isUntapped() && (haste || !c.hasSickness()) && !c.isCreature());
        final int landAttackers = lands.size() - cost;
        if (landAttackers <= 0) {
            return no(AiPlayDecision.CantAfford);
        }
        final List<Integer> powers = new ArrayList<>();
        for (int i = 0; i < landAttackers; i++) {
            powers.add(2);
        }
        for (final Card c : ai.getCreaturesInPlay()) {
            if (CombatUtil.canAttack(c)) {
                powers.add(Math.max(0, c.getNetCombatDamage()));
            }
        }
        powers.sort(Comparator.reverseOrder());
        for (final Player opp : ai.getOpponents()) {
            int blockers = CardLists.count(opp.getCreaturesInPlay(), Card::isUntapped);
            if (everyones) {
                blockers += CardLists.count(opp.getLandsInPlay(), c -> c.isUntapped() && !c.isCreature());
            }
            final int dmg = powers.stream().skip(blockers).mapToInt(Integer::intValue).sum();
            if (opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife() && dmg >= opp.getLife()) {
                return YES;
            }
        }
        return no(AiPlayDecision.CantPlayAi);
    }

    // what a card is worth to them back in hand
    private static int gift(final Card c) {
        return c.isLand() ? 20 : 30 + c.getCMC() * 15;
    }

    private static AiAbilityDecision giveBack(final Player ai, final SpellAbility sa) {
        final SpellAbility next = sa.getSubAbility();
        if (next == null || !sa.usesTargeting() || !next.usesTargeting()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        next.resetTargets();
        Card payoff = null;
        int worth = 0;
        if (next.getApi() == ApiType.Destroy) {
            // Nullmage Advocate: their best artifact or enchantment
            for (final Card c : CardUtil.getValidCardsToTarget(next)) {
                final int v = ExchangeTradeAi.value(c);
                if (c.getController().isOpponentOf(ai) && c.canBeDestroyed() && v > worth) {
                    payoff = c;
                    worth = v;
                }
            }
        } else if (next.getApi() == ApiType.ChangeZone) {
            // Pulsemage Advocate: its best creature back from its graveyard
            for (final Card c : CardUtil.getValidCardsToTarget(next)) {
                final int v = ComputerUtilCard.evaluateCreature(c);
                if (c.getOwner().equals(ai) && v > worth) {
                    payoff = c;
                    worth = v;
                }
            }
        }
        if (payoff == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        final int n = sa.getMinTargets();
        for (final Player opp : ai.getOpponents()) {
            final List<Card> theirs = new ArrayList<>(CardLists.filter(opp.getCardsIn(ZoneType.Graveyard), sa::canTarget));
            if (theirs.size() < n) {
                continue;
            }
            theirs.sort(Comparator.comparingInt(LandAlphaGiveBackAi::gift));
            final List<Card> given = theirs.subList(0, n);
            if (worth - given.stream().mapToInt(LandAlphaGiveBackAi::gift).sum() < 60) {
                continue;
            }
            given.forEach(c -> sa.getTargets().add(c));
            next.getTargets().add(payoff);
            return YES;
        }
        return no(AiPlayDecision.TargetingFailed);
    }
}
