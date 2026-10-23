package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

/**
 * Snap Judgment: "The player who wins this game also wins the match." A draw decides nothing (rulings), and a match
 * this game would decide anyway is unchanged.
 */
public class DecideMatchEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "The player who wins this game also wins the match.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        game.setWinnerWinsMatch(true);
        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, "The player who wins this game also wins the match.");
    }
}
