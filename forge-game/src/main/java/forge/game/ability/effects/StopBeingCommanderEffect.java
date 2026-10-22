package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Zassacre Zirl, The Highland Runner: a card stops being its owner's commander for the rest of the game. With
 * MinCommanders$ N, only if its owner has at least N commanders.
 */
public class StopBeingCommanderEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return getTargetCards(sa) + " stops being a commander.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final int min = Integer.parseInt(sa.getParamOrDefault("MinCommanders", "1"));
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card current = c.getGame().getCardState(c, null);
            final Card card = current == null ? c : current;
            final Player owner = card.getOwner();
            if (owner == null || owner.getCommanders().size() < min || !owner.getCommanders().contains(card)) {
                continue;
            }
            owner.removeCommander(card);
            card.getGame().getAction().notifyOfValue(sa, card, card + " is no longer a commander.", null);
        }
    }
}
