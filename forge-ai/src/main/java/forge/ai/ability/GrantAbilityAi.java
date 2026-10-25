package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ GrantAbility: spells whose whole effect is an ability or trigger handed to creatures until end of turn, so
 * the spell is worth it only when the AI will use what it hands out.
 * <ul>
 * <li>Resuscitate ("{1}: Regenerate"): its creatures that would die this combat or to a spell on the stack, with mana
 * left to regenerate them.</li>
 * <li>Showstopper (dies: 2 damage to their creature): its creatures that would die this combat, with targets for the
 * damage.</li>
 * <li>Shade's Breath ("{B}: +1/+1"): its attackers or blockers after blocks, with black mana left to pump.</li>
 * <li>Retraction Helix ("{T}: return a nonland permanent"): an untapped creature able to tap, on its weakest one, when
 * they have something worth bouncing.</li>
 * </ul>
 */
public final class GrantAbilityAi {

    private GrantAbilityAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static String granted(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        for (final String key : new String[] {"Abilities", "Triggers"}) {
            for (final String name : sa.getParamOrDefault(key, "").split(",")) {
                if (!name.isEmpty()) {
                    sb.append(sa.getSVar(name)).append('\n');
                    // a trigger's effect is a further SVar
                    final java.util.regex.Matcher m = java.util.regex.Pattern.compile("Execute\\$ (\\w+)").matcher(sa.getSVar(name));
                    if (m.find()) {
                        sb.append(sa.getSVar(m.group(1))).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    // its creatures that will die this combat or to what's on the stack
    private static List<Card> doomed(final Player ai) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final List<GameObject> threatened = game.getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
        return CardLists.filter(ai.getCreaturesInPlay(), c -> threatened.contains(c)
                || combat != null && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && (combat.isAttacking(c) || combat.isBlocking(c)) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat));
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final String what = granted(sa);
        final int spare = ComputerUtilMana.getAvailableManaEstimate(ai, false)
                - (sa.getPayCosts() != null && sa.getPayCosts().getTotalMana() != null ? sa.getPayCosts().getTotalMana().getCMC() : 0);
        if (what.contains("Regenerate")) {
            final List<Card> doomed = CardLists.filter(doomed(ai), Card::canBeDestroyed);
            final int worth = doomed.stream().limit(Math.max(0, spare)).mapToInt(ComputerUtilCard::evaluateCreature).sum();
            return worth >= 150 ? YES : no(AiPlayDecision.AnotherTime);
        }
        if (what.contains("DealDamage") && what.contains("Destination$ Graveyard")) {
            final List<Card> doomed = doomed(ai);
            final boolean targets = ai.getOpponents().getCreaturesInPlay().stream().anyMatch(c -> c.getNetToughness() - c.getDamage() <= 2 * doomed.size());
            return !doomed.isEmpty() && targets ? YES : no(AiPlayDecision.AnotherTime);
        }
        if (what.contains("AB$ Pump") && what.contains("Cost$ B")) {
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            final Combat combat = ai.getGame().getCombat();
            final boolean fighting = combat != null && ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && ai.getCreaturesInPlay().stream().anyMatch(c -> combat.isAttacking(c) || combat.isBlocking(c));
            final int black = CardLists.count(ai.getCardsIn(ZoneType.Battlefield),
                    c -> c.isUntapped() && c.isLand() && c.getManaAbilities().stream().anyMatch(m -> m.canProduce("B")));
            // one black for the spell, two or more to pump with
            return fighting && black >= 3 && spare >= 2 ? YES : no(AiPlayDecision.WaitForCombat);
        }
        if (what.contains("Destination$ Hand") && what.contains("Cost$ T") && sa.usesTargeting()) {
            final boolean worthBouncing = ai.getOpponents().getCardsIn(ZoneType.Battlefield).stream()
                    .anyMatch(c -> !c.isLand() && (c.isToken() || c.getCMC() >= 4 || !c.getAttachedCards().isEmpty()));
            final Card tapper = ComputerUtilCard.getWorstCreatureAI(CardLists.filter(ai.getCreaturesInPlay(),
                    c -> c.isUntapped() && !c.isSick() && sa.canTarget(c)));
            if (!worthBouncing || tapper == null) {
                return no(AiPlayDecision.TargetingFailed);
            }
            sa.resetTargets();
            sa.getTargets().add(tapper);
            return YES;
        }
        return no(AiPlayDecision.CantPlayAi);
    }
}
