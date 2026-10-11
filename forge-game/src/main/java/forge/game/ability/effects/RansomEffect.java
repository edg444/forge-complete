package forge.game.ability.effects;

import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Squidnapper's "Ransom — {6} and 2 life": a special action for the player whose creature it holds; paying ends the
 * control effects holding their creatures ({@code LoseControl$ Ransom} on the GainControl).
 */
public class RansomEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player payer = sa.getActivatingPlayer();
        host.payRansom(payer);
        host.getGame().getAction().checkStateEffects(true);
        host.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, payer + " pays " + host + "'s ransom.");
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " pays the ransom.";
    }
}
