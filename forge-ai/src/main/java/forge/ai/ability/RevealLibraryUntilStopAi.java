package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/**
 * Push Your Luck: aiming for a total of exactly 7 without going over, the AI reveals another card while its total is 4
 * or less - one more card is usually two or three.
 */
public class RevealLibraryUntilStopAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(final Player player, final SpellAbility sa, final PlayerActionConfirmMode mode,
            final String message, final Map<String, Object> params) {
        final String total = sa.getHostCard().getSVar("RevealedMV").replace("Number$", "");
        return total.isEmpty() || Integer.parseInt(total) <= 4;
    }
}
