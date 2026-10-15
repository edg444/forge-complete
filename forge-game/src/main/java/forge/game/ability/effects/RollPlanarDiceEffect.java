package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameType;
import forge.game.PlanarDice;
import forge.game.ability.SpellAbilityEffect;
import forge.game.event.GameEventRollDie;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class RollPlanarDiceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (game.getActivePlanes() == null) { // not a planechase game, nothing happens
            return;
        }
        if (sa.hasParam("SpecialAction")) {
            game.getPhaseHandler().incPlanarDiceSpecialActionThisTurn();
        }
        PlanarDice result = PlanarDice.roll(activator, null);
        game.fireEvent(new GameEventRollDie(result));
        String message = Localizer.getInstance().getMessage("lblPlanarDiceResult", result.toString());
        game.getAction().notifyOfValue(sa, activator, message, null);
        // Fractured Powerstone outside Planechase: you planeswalk away, but there's no planar deck to go to
        if (result == PlanarDice.Planeswalk && !game.getRules().hasAppliedVariant(GameType.Planechase)) {
            for (Player p : game.getPlayers()) {
                p.leaveCurrentPlane();
            }
        }
    }
}
