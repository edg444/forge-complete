package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Squidnapper's ransom: buy back a creature worth the {6} and 2 life, when the life can be spared. */
public class RansomAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        if (ai.getLife() <= 8) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        int value = 0;
        for (final Card c : sa.getHostCard().getHeldForRansom(ai)) {
            value += ComputerUtilCard.evaluateCreature(c);
        }
        // roughly a three-mana creature's worth or more
        if (value >= 160) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
