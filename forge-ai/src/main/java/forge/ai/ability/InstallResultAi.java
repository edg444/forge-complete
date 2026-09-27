package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Socketed Sprocketer's "uninstall all results, then roll and install". Worth it when nothing good is installed
 * (a 4 or better is kept for later rolls, a 6 for a card), and done at the end of an opponent's turn or in its own
 * second main phase, when the tapped creature costs nothing.
 */
public class InstallResultAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if ("All".equals(sa.getParam("Uninstall"))
                && host.getInstalledResults().stream().anyMatch(r -> r >= 4)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean oppEndStep = !ph.isPlayerTurn(ai) && ph.is(PhaseType.END_OF_TURN);
        final boolean ownMain2 = ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2);
        if (!oppEndStep && !ownMain2) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
