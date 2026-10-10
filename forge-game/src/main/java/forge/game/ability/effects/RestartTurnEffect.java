package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

/**
 * Do-Over: "Restart the turn, except with CARDNAME in exile." The restart itself waits until the spell has finished
 * resolving (see {@link forge.game.TurnRestart}); the stack is part of what goes back to how the turn began.
 */
public class RestartTurnEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        sa.getHostCard().getGame().getPhaseHandler().requestTurnRestart(sa.getHostCard());
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Restart the turn, except with " + sa.getHostCard().getName() + " in exile.";
    }
}
