package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sole Performer: worth {T}{T} only with another permanent whose non-mana {T} ability the AI could then use - one
 * already tapped, or one it would rather keep untapped. In its own main phase, so the {T}s get spent before the step
 * ends and they're gone.
 */
public class AddTapSymbolsAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getPhaseHandler().isPlayerTurn(ai) || !ai.getGame().getPhaseHandler().getPhase().isMain()
                || !ai.getGame().getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.equals(sa.getHostCard()) || c.isAbilitySick()) {
                continue;
            }
            for (final SpellAbility other : c.getSpellAbilities()) {
                if (other.isActivatedAbility() && !other.isManaAbility() && other.getPayCosts() != null
                        && other.getPayCosts().hasTapCost()) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
    }
}
