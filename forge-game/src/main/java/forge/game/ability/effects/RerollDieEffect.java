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
 * Pippa, Duchess of Dice: "Reroll any die" when it's a die on the battlefield - a Die creature can be rerolled any time
 * (rulings), and its new face is its power and toughness. Rerolling is rolling, so the roll goes through
 * RollDiceEffect (modifiers, "whenever you roll" triggers). Rerolling a die someone just rolled is handled during that
 * roll instead (RollDiceEffect.payToRerollAnyDie).
 */
public class RerollDieEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " rerolls a die.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final CardCollection dice = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Card::isDie);
        if (dice.isEmpty()) {
            return;
        }
        final Card die = p.getController().chooseSingleEntityForEffect(dice, sa, "Choose a die to reroll", null);
        if (die == null) {
            return;
        }
        final int face = RollDiceEffect.rollDiceForPlayer(sa, p, 1, die.getDieSides());
        die.setDieFace(face, die.getDieSides());
        game.getGameLog().add(GameLogEntryType.INFORMATION, p + " rerolls " + die + ": it now shows " + face + ".");
    }
}
