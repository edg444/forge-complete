package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ Radiance (Cleansing Beam, Wojek Embermage, Leave No Trace, Incite Hysteria, Wojek Siren): the effect reaches
 * the target and everything else of its kind sharing a color with it, own permanents included, so each possible target
 * is judged by the whole group it brings.
 */
public final class RadianceAi {

    private RadianceAi() { }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : (c.getCMC() + 1) * 25;
    }

    private static int side(final Player ai, final Card c) {
        return c.getController().isOpponentOf(ai) ? 1 : c.getController().equals(ai) ? -1 : 0;
    }

    private static CardCollection group(final Player ai, final Card t) {
        final CardCollection out = new CardCollection(t);
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!c.equals(t) && c.sharesColorWith(t) && (t.isCreature() ? c.isCreature() : c.sharesCardTypeWith(t))) {
                out.add(c);
            }
        }
        return out;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Combat combat = ai.getGame().getCombat();
        sa.resetTargets();
        int best = Integer.MIN_VALUE;
        Card bestT = null;
        // Brightflame: X is picked with the target
        final boolean xDamage = sa.getApi() == forge.game.ability.ApiType.DealDamage && "Count$xPaid".equals(sa.getSVar(sa.getParamOrDefault("NumDmg", "")));
        final int maxX = !xDamage ? 0 : forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai, false)
                - (sa.getPayCosts().getTotalMana() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC());
        int bestX = 0;
        for (final Card t : CardUtil.getValidCardsToTarget(sa)) {
            final CardCollection g = group(ai, t);
            int v = 0;
            int xHere = 0;
            switch (sa.getApi()) {
                case DealDamage -> {
                    if (xDamage) {
                        int bestHere = Integer.MIN_VALUE;
                        for (int x = 1; x <= maxX; x++) {
                            int here = -x * 5;
                            for (final Card c : g) {
                                if (x >= c.getLethalDamage() && c.canBeDestroyed()) {
                                    here += side(ai, c) * worth(c);
                                }
                            }
                            if (here > bestHere) {
                                bestHere = here;
                                xHere = x;
                            }
                        }
                        v = bestHere;
                        break;
                    }
                    final int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumDmg"), sa);
                    for (final Card c : g) {
                        if (dmg >= c.getLethalDamage() && c.canBeDestroyed()) {
                            v += side(ai, c) * worth(c);
                        }
                    }
                }
                case Destroy -> {
                    for (final Card c : g) {
                        if (c.canBeDestroyed()) {
                            v += side(ai, c) * worth(c);
                        }
                    }
                }
                case Pump -> {
                    if (sa.getParamOrDefault("KW", "").contains("can't block")) {
                        // Incite Hysteria: its first main phase, for an attack the group can't block
                        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
                            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
                        }
                        final Player opp = ai.getWeakestOpponent();
                        if (opp == null || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
                            continue;
                        }
                        final List<Card> attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
                        final CardCollection blockers = CardLists.filter(opp.getCreaturesInPlay(), CombatUtil::canBlock);
                        final CardCollection left = new CardCollection(blockers);
                        left.removeAll(g);
                        if (ForcedCombatAi.assault(attackers, blockers, opp) < opp.getLife()
                                && ForcedCombatAi.assault(attackers, left, opp) >= opp.getLife()) {
                            v = 1000;
                        }
                    } else {
                        // Wojek Siren: once blocks are known, for the creatures fighting on each side
                        if (combat == null || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
                        }
                        for (final Card c : g) {
                            if (combat.isAttacking(c) || combat.isBlocking(c)) {
                                v -= side(ai, c) * 40;
                            }
                        }
                    }
                }
                default -> {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
            if (v > best) {
                best = v;
                bestT = t;
                bestX = xHere;
            }
        }
        final int threshold = sa.isSpell() ? 60 : 30;
        if (bestT == null || best < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (xDamage) {
            sa.setXManaCostPaid(bestX);
        }
        sa.getTargets().add(bestT);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
