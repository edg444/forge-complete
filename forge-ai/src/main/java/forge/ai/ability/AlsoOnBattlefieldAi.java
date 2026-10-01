package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Masterful Ninja's "is on the battlefield and in your hand until end of turn". It's back in hand at end of turn
 * whatever happens, so the only reasons to bring it in are combat ones: on our turn to attack with it (it has
 * haste), on an opponent's turn once attackers are declared, to block one it beats or when the attack is
 * dangerous. Any other time it would only be exposed to removal.
 */
public class AlsoOnBattlefieldAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!host.isInZone(ZoneType.Hand) || !host.isCreature()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.isPlayerTurn(ai)) {
            if ((ph.is(PhaseType.MAIN1) || ph.is(PhaseType.COMBAT_BEGIN))
                    && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, host)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }

        final Combat combat = ai.getGame().getCombat();
        if (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) || combat == null) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final boolean danger = ComputerUtilCombat.lifeInDanger(ai, combat);
        for (final Card attacker : combat.getAttackers()) {
            if (!ai.equals(combat.getDefenderPlayerByAttacker(attacker)) || !CombatUtil.canBlock(attacker, host)) {
                continue;
            }
            if (danger || (ComputerUtilCombat.canDestroyAttacker(ai, attacker, host, combat, false)
                    && !ComputerUtilCombat.canDestroyBlocker(ai, host, attacker, combat, false))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
