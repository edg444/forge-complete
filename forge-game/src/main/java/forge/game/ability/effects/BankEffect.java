package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Bank: the resolving spell is exiled until the end of the match instead of going to the graveyard. The match then
 * starts each later game with it in exile, castable from there ({@code Match}), for as long as it stays exiled.
 */
public class BankEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Card banked = game.getAction().exile(host, sa, null);
        if (banked != null) {
            banked.setBank(1);
            game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, banked.getOwner() + " banks " + banked + " for the rest of the match.");
        }
    }
}
