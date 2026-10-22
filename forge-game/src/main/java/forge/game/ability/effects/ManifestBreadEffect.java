package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import forge.GameCommand;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * You're Toast!: manifest bread is manifest dread, except the face-down card is a Food artifact with "{2}, {T},
 * Sacrifice this artifact: You gain 3 life." instead of a 2/2 creature, and it can't be turned face up
 * (Card.canBeTurnedFaceUp). It's bread.
 */
public class ManifestBreadEffect extends ManifestDreadEffect {

    private static final String FOOD = "AB$ GainLife | Cost$ 2 T Sac<1/CARDNAME/this artifact> | LifeAmount$ 3 | SpellDescription$ You gain 3 life.";

    @Override
    protected Card internalEffect(final Card c, final Player p, final SpellAbility sa, final Map<AbilityKey, Object> moveParams) {
        final Card bread = super.internalEffect(c, p, sa, moveParams);
        if (bread == null || !bread.isFaceDown()) {
            return bread;
        }
        final CardState faceDown = bread.getFaceDownState();
        faceDown.setType(new CardType(List.of("Artifact", "Food"), false));
        faceDown.addSpellAbility(AbilityFactory.getAbility(FOOD, bread));
        bread.addLeavesPlayCommand(new GameCommand() {
            private static final long serialVersionUID = 1L;

            @Override
            public void run() {
                // the next time it's face down it's an ordinary face-down card again
                bread.clearStates(CardStateName.FaceDown, true);
            }
        });
        bread.updateStateForView();
        return bread;
    }
}
