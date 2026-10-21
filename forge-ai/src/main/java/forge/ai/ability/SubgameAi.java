package forge.ai.ability;

import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Subgames are always worth playing. Tug of War's cards for the subgame battlefield, and the one the winner keeps: the
 * best of them.
 */
public class SubgameAi extends AlwaysPlayAi {

    @Override
    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, final Iterable<Card> options, final boolean isOptional,
            final Player targetedPlayer, final Map<String, Object> params) {
        return ComputerUtilCard.getBestAI(new CardCollection(options));
    }
}
