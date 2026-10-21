package forge.ai.ability;

import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.WordList;

/**
 * Spelling Bee: the AI chooses the hardest word for the other player to spell - the longest, and of those, one that
 * isn't a common English word.
 */
public class SpellChosenWordAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public String chooseString(final Player ai, final SpellAbility sa, final List<String> options) {
        return options.stream().max(Comparator.<String>comparingInt(String::length)
                .thenComparing(w -> !WordList.isCommon(w.toLowerCase()))).orElse(null);
    }
}
