package forge.game.ability.effects;

import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

/**
 * The Commander Recommender: the Defined$ players guessed wrong, for "whenever an opponent guesses wrong" triggers.
 * Scripts whose guess is worked out in their own sub-abilities (Liar's Pendulum) use this; guess effects call
 * {@link #fire} themselves.
 */
public class GuessedWrongEffect extends SpellAbilityEffect {

    public static void fire(final Player guesser) {
        guesser.getGame().getTriggerHandler().runTrigger(TriggerType.GuessedWrong, AbilityKey.mapFromPlayer(guesser), false);
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            fire(p);
        }
    }
}
