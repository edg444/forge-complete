package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ShootAtLibraryAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        return pickTarget(ai, sa, false)
                ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return pickTarget(ai, sa, mandatory)
                ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    private static boolean pickTarget(Player ai, SpellAbility sa, boolean mandatory) {
        if (!sa.usesTargeting()) {
            return true;
        }
        sa.resetTargets();
        // every card knocked off is also half a point of damage, so the lowest life total gains the most
        PlayerCollection opps = ai.getOpponents().filter(p -> sa.canTarget(p) && !p.getCardsIn(ZoneType.Library).isEmpty());
        if (!opps.isEmpty()) {
            sa.getTargets().add(opps.min(PlayerPredicates.compareByLife()));
            return true;
        }
        if (mandatory && sa.canTarget(ai)) {
            sa.getTargets().add(ai);
            return true;
        }
        return false;
    }
}
