package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Do-Over: restart the turn when this turn has gone worse for the AI than for its opponents - life lost and permanents
 * lost, each side's against the other's - by enough to be worth the card. The later in the turn, the less it takes,
 * since there's less turn left to turn things around in.
 */
public class RestartTurnAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        if (!game.getPhaseHandler().canRestartTurn()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final int swing = turnSwing(ai);
        final boolean late = game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_END);
        if (swing >= (late ? 4 : 8) || (swing > 0 && ComputerUtil.aiLifeInDanger(ai, true, 0))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /** How much more this turn has cost the AI than its opponents. */
    static int turnSwing(final Player ai) {
        int swing = lost(ai);
        int worstOpponent = 0;
        for (final Player opp : ai.getOpponents()) {
            worstOpponent = Math.max(worstOpponent, lost(opp));
        }
        return swing - worstOpponent;
    }

    /** Life lost this turn, plus what each permanent the player had and no longer has was worth. */
    private static int lost(final Player p) {
        int lost = Math.max(0, p.getLifeStartedThisTurnWith() - p.getLife());
        for (final Card c : p.getGame().getLeftBattlefieldThisTurn()) {
            if (c.getController() == p && !c.isLand()) {
                lost += c.isToken() ? 1 : 2 + c.getCMC();
            }
        }
        return lost;
    }
}
