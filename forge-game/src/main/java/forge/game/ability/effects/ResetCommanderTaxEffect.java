package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Shedding Snake: the commander tax on each of the defined players' commanders is reduced back to zero. Only the tax
 * starts over; the number of times each commander has been cast from the command zone stays as it was.
 */
public class ResetCommanderTaxEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "The commander tax on each of " + getDefinedPlayersOrTargeted(sa) + "'s commanders is reduced back to zero.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            p.resetCommanderTax();
            p.getGame().getGameLog().add(forge.game.GameLogEntryType.STACK_RESOLVE,
                    "The commander tax on " + p + "'s commanders is back to zero.");
        }
    }
}
