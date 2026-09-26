package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.AnimateLibraryEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AnimateLibraryAi extends SpellAbilityAi {

    // a six-mana Aura should at least make a sizeable creature; it only shrinks as the game goes on
    private static final int MIN_LIBRARY_SIZE = 6;

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // a second one only stacks the same protection on the same library
        if (AnimateLibraryEffect.findLibraryPermanent(ai) != null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (ai.getCardsIn(ZoneType.Library).size() < MIN_LIBRARY_SIZE) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
