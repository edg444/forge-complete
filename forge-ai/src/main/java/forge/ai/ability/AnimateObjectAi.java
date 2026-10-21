package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.CounterEnumType;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Animate Object: worth casting only if a power/toughness sticker will be there for the object - without one it would
 * be a 0/0. The seven tickets it gives come first, so they count toward what's affordable.
 */
public class AnimateObjectAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final int tickets = ai.getCounters(CounterEnumType.TICKET) + 7;
        for (final Sticker s : StickerSheet.getAvailableStickers(ai, tickets)) {
            if (s.getKind() == StickerKind.PT && s.getToughness() > 0) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
    }
}
