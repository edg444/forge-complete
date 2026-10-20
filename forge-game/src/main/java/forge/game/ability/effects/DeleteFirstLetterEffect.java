package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.NameLetters;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Stet, Draconic Proofreader: "Delete the first letter of target permanent or player's name until end of turn." A
 * permanent's deleted name is a name change (it overrides a flavor name too); a player's is their name in the game
 * until end of turn, all deletions undone together.
 */
public class DeleteFirstLetterEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Delete the first letter of " + getTargetEntities(sa) + "'s name until end of turn.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        for (final GameEntity e : getTargetEntities(sa)) {
            if (e instanceof Card c) {
                final Card gc = game.getCardState(c);
                if (!gc.isInPlay()) {
                    continue;
                }
                final String now = NameLetters.deleteFirstLetter(gc.getDisplayName());
                final long ts = game.getNextTimestamp();
                gc.addChangedName(now, false, ts, 0);
                game.getEndOfTurn().addUntil(() -> gc.removeChangedName(ts, 0));
                game.getGameLog().add(GameLogEntryType.INFORMATION, "Its name is now \"" + now + "\" until end of turn.");
            } else if (e instanceof Player p) {
                p.deleteFirstLetterOfName();
                game.getEndOfTurn().addUntil(p::restoreDeletedName);
                game.getGameLog().add(GameLogEntryType.INFORMATION, "A player's name is now \"" + p.getName()
                        + "\" until end of turn.");
            }
        }
    }
}
