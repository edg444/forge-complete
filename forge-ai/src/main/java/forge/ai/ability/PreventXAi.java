package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ PreventX (Festival of the Guildpact, Guardian Angel): "prevent the next X damage", X paid in mana, so just
 * the X that matters once blocks are known - its creature that would die, or the damage that would kill it. Festival
 * of the Guildpact also draws a card, so with nothing to prevent it goes at their end step with X of 0.
 */
public final class PreventXAi {

    private PreventXAi() { }

    private static AiAbilityDecision yes(final SpellAbility sa, final int x) {
        sa.setXManaCostPaid(x);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final Combat combat = game.getCombat();
        sa.setXManaCostPaid(null);
        final int mana = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        if (combat != null && ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            if (sa.usesTargeting()) {
                sa.resetTargets();
                // Guardian Angel: its best creature in the fight that just enough prevention saves
                Card best = null;
                int bestX = 0;
                for (final Card c : ai.getCreaturesInPlay()) {
                    if (!sa.canTarget(c) || !combat.isAttacking(c) && !combat.isBlocking(c)
                            || !ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                        continue;
                    }
                    int dmg = 0;
                    for (final Card o : combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c)) {
                        dmg += o.getNetCombatDamage();
                    }
                    final int x = dmg - (c.getNetToughness() - c.getDamage()) + 1;
                    if (x > 0 && x <= mana && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                        best = c;
                        bestX = x;
                    }
                }
                if (best != null) {
                    sa.getTargets().add(best);
                    return yes(sa, bestX);
                }
            }
            if (!ph.isPlayerTurn(ai) && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                // enough to stay above 0, or as much as it can pay for
                final int x = Math.min(mana, Math.max(1, 1 - ComputerUtilCombat.lifeThatWouldRemain(ai, combat)));
                if (sa.usesTargeting()) {
                    if (!sa.canTarget(ai)) {
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }
                    sa.getTargets().add(ai);
                }
                return yes(sa, x);
            }
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        // Festival of the Guildpact: a cantrip with nothing better to do
        final boolean draws = sa.getSubAbility() != null && sa.getSubAbility().getApi() == forge.game.ability.ApiType.Draw;
        if (draws && !sa.usesTargeting() && ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai)) {
            return yes(sa, 0);
        }
        return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
    }
}
