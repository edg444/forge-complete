package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Problematic Volcano: puts creatures to the left or right of the host. {@code AllPlayers$ True}: starting with the
 * activator, each player assigns the creatures they control, seeing the choices before theirs. {@code Defined$}: each
 * card's controller picks a side for it (creatures entering). {@code Clear$ True}: the volcano left, so its sides go.
 */
public class ChooseSideEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card volcano = sa.getHostCard();
        final Game game = volcano.getGame();

        if (sa.hasParam("Clear")) {
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.getVolcanoSide(volcano) != null) {
                    c.setVolcanoSide(volcano, null);
                }
            }
            return;
        }

        if (sa.hasParam("AllPlayers")) {
            for (final Player p : game.getPlayersInTurnOrder(sa.getActivatingPlayer())) {
                final CardCollection creatures = new CardCollection(p.getCreaturesInPlay());
                if (creatures.isEmpty()) {
                    continue;
                }
                final CardCollectionView left = p.getController().chooseCardsForEffect(creatures, sa,
                        "Choose the creatures to put to the left of " + volcano.getName() + " (the rest go to the right)",
                        0, creatures.size(), true, null);
                final CardCollection right = new CardCollection(creatures);
                right.removeAll(left);
                for (final Card c : creatures) {
                    c.setVolcanoSide(volcano, left.contains(c));
                }
                game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, p + " puts " + describe(left) + " to the left and "
                        + describe(right) + " to the right of " + volcano.getName() + ".");
            }
            return;
        }

        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Player chooser = c.getController();
            final boolean left = chooser.getController().chooseBinary(sa,
                    "Put " + c.getName() + " to the left or right of " + volcano.getName() + "?", BinaryChoiceType.LeftOrRight);
            c.setVolcanoSide(volcano, left);
            game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, chooser + " puts " + c.getName() + " to the "
                    + (left ? "left" : "right") + " of " + volcano.getName() + ".");
        }
    }

    private static String describe(final CardCollectionView cards) {
        return cards.isEmpty() ? "nothing" : cards.toString();
    }
}
