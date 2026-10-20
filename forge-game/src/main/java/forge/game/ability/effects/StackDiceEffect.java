package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Boomstacker: "stack two dice on top of it" - Amount$ dice onto each defined card's stack (Card.stackDice). */
public class StackDiceEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Stack " + sa.getParamOrDefault("Amount", "1") + " dice on top of " + sa.getHostCard() + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "1"), sa);
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            c.stackDice(amount);
        }
    }
}
