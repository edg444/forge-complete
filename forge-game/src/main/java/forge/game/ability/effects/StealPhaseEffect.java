package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Unstable's Clocknapper: "Steal that phase from target player during their next turn. (That phase occurs as
 * though it's your turn instead.)" Phase$ is one of {@link PhaseHandler#STEALABLE_PHASE_NAMES}.
 */
public class StealPhaseEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String label = PhaseHandler.stolenPhaseLabel(PhaseHandler.STEALABLE_PHASE_NAMES.indexOf(sa.getParam("Phase")));
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            sb.append(sa.getActivatingPlayer()).append(" steals ").append(label).append(" from ").append(p)
                    .append(" during their next turn. ");
        }
        return sb.toString().trim();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player thief = sa.getActivatingPlayer();
        final String phase = sa.getParam("Phase");
        for (final Player victim : getDefinedPlayersOrTargeted(sa)) {
            if (!victim.isInGame()) {
                continue;
            }
            thief.getGame().getPhaseHandler().stealPhase(thief, victim, phase);
        }
    }
}
