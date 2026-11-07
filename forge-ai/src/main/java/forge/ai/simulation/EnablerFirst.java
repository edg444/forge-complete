package forge.ai.simulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.game.CardTraitBase;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * A permanent cast before the spell whose size it raises: Noble's Purse adds a counter kind, so Storm of Forms cast
 * after it gets one more copy. Neither the heuristics' order (highest cost first, and noncreature permanents wait for
 * Main 2) nor a phone's few rollouts see it. Fork-only.
 */
public final class EnablerFirst {
    // LifeAmount is left out: a higher count there is as often life lost as life gained
    private static final String[] AMOUNT_PARAMS = { "Amount", "NumDmg", "NumCards", "TokenAmount", "CounterNum", "NumAtt", "NumDef" };
    private static final int MAX_TRIED = 2;

    private EnablerFirst() {
    }

    public static SpellAbility before(final Player ai, final SpellAbility payoff, final List<SpellAbility> plays,
                                      final Predicate<SpellAbility> acceptable) {
        final Card host = payoff.getHostCard();
        if (host == null || !payoff.isSpell() || isPermanentSpell(payoff) || payoff.getPayCosts() == null
                || payoff.getPayCosts().hasXInAnyCostPart() || !ai.getGame().getStack().isEmpty() || !ai.canCastSorcery()) {
            return null;
        }
        final Set<String> counts = scalingCounts(payoff);
        if (counts.isEmpty()) {
            return null;
        }
        final int payoffCmc = payoff.getPayCosts().getTotalMana().getCMC();
        final int mana = ComputerUtilMana.getAvailableManaEstimate(ai, true);
        final int before = total(host, counts, payoff);
        int tried = 0;
        for (final SpellAbility sa : plays) {
            final Card c = sa.getHostCard();
            if (c == null || c == host || !isPermanentSpell(sa) || sa.getPayCosts() == null
                    || sa.getPayCosts().getTotalMana().getCMC() + payoffCmc > mana) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            if (!sa.canPlay() || !ComputerUtilCost.canPayCost(sa, ai, false) || !acceptable.test(sa)) {
                continue;
            }
            if (raisesCount(ai, payoff, sa, counts, before)) {
                return sa;
            }
            if (++tried >= MAX_TRIED) {
                break;
            }
        }
        return null;
    }

    private static boolean isPermanentSpell(final SpellAbility sa) {
        return sa.getApi() == ApiType.PermanentCreature || sa.getApi() == ApiType.PermanentNoncreature;
    }

    private static Set<String> scalingCounts(final SpellAbility payoff) {
        final Card host = payoff.getHostCard();
        final Set<String> names = new HashSet<>();
        for (SpellAbility a = payoff; a != null; a = a.getSubAbility()) {
            addCounts(a, names);
        }
        for (final Trigger t : host.getTriggers()) {
            if ((t.getMode() == TriggerType.SpellCast || t.getMode() == TriggerType.SpellCastOrCopy)
                    && "Card.Self".equals(t.getParam("ValidCard"))) {
                for (SpellAbility a = t.ensureAbility(); a != null; a = a.getSubAbility()) {
                    addCounts(a, names);
                }
            }
        }
        return names;
    }

    private static void addCounts(final SpellAbility a, final Set<String> names) {
        for (final String p : AMOUNT_PARAMS) {
            final String name = a.getParam(p);
            if (name != null && a.hasSVar(name)) {
                final String svar = a.getSVar(name);
                if (svar.startsWith("Count$") && !svar.contains("xPaid")) {
                    names.add(name);
                }
            }
        }
    }

    private static int total(final Card host, final Set<String> counts, final CardTraitBase ctb) {
        int sum = 0;
        for (final String name : counts) {
            sum += AbilityUtils.calculateAmount(host, name, ctb);
        }
        return sum;
    }

    private static boolean raisesCount(final Player ai, final SpellAbility payoff, final SpellAbility enabler,
                                       final Set<String> counts, final int before) {
        final GameSimulator sim = GameSimulator.forRollout(ai.getGame(), ai, false);
        if (!sim.applySpellAbility(enabler)) {
            return false;
        }
        final Card simHost = sim.getGameCopier().find(payoff.getHostCard());
        final Player simAi = sim.getSimulatedAiPlayer();
        if (simHost == null || simHost.getZone() == null || simHost.getZone().getZoneType() != payoff.getHostCard().getZone().getZoneType()) {
            return false;
        }
        for (final SpellAbility simSa : simHost.getSpellAbilities()) {
            if (simSa.getDescription().equals(payoff.getDescription())) {
                simSa.setActivatingPlayer(simAi);
                return total(simHost, counts, simSa) > before && ComputerUtilCost.canPayCost(simSa, simAi, false);
            }
        }
        return false;
    }
}
