package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Syx, Robot Overlord: the target opponent may point at a creature its controller controls and call it an invader
 * (one of the names written down as the game began). Right: that creature is exiled. Wrong: they lose 3 life.
 * User ruling 2026-10-22: guessing right means calling an invader an invader.
 */
public class GuessInvaderEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return getTargetPlayers(sa) + " may guess whether a creature " + sa.getActivatingPlayer() + " controls is an invader.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player owner = sa.getActivatingPlayer();
        final Game game = owner.getGame();
        final CardCollection creatures = CardLists.filter(owner.getCardsIn(ZoneType.Battlefield), Card::isCreature);
        if (creatures.isEmpty()) {
            return;
        }
        for (final Player guesser : getTargetPlayers(sa)) {
            if (!guesser.isInGame()) {
                continue;
            }
            // The Fact Checker: the guesser just chooses how it comes out - right means picking one of the invaders
            final Boolean outcome = forge.game.staticability.StaticAbilityChooseGuessOutcome.chosenOutcome(guesser, sa);
            final CardCollection invaders = CardLists.filter(creatures, owner::isInvader);
            if (outcome != null && (!outcome || invaders.isEmpty())) {
                continue;
            }
            final Card guess = guesser.getController().chooseSingleEntityForEffect(outcome != null ? invaders : creatures, sa,
                    "Call one of " + owner + "'s creatures an invader (or don't guess)", true, null);
            if (guess == null) {
                continue;
            }
            if (owner.isInvader(guess)) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, guesser + " is right: " + guess + " is an invader.");
                game.getAction().exile(guess, sa, null);
            } else {
                game.getGameLog().add(GameLogEntryType.INFORMATION, guesser + " is wrong: " + guess + " isn't an invader.");
                GuessedWrongEffect.fire(guesser);
                guesser.loseLife(3, false, false, sa);
            }
        }
    }
}
