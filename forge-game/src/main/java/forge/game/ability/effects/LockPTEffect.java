package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Just a Li'l Guy: the targeted creature is Power$/Toughness$ until end of turn and can't be anything else until then -
 * no counter, pump, setting or switching changes it.
 */
public class LockPTEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return getTargetCards(sa) + " is " + sa.getParam("Power") + "/" + sa.getParam("Toughness") + " until end of turn.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        final int toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card cur = game.getCardState(c);
            if (!cur.isInPlay()) {
                continue;
            }
            cur.lockPT(power, toughness);
            game.getEndOfTurn().addUntil(new GameCommand() {
                private static final long serialVersionUID = 1L;

                @Override
                public void run() {
                    cur.lockPT(null, null);
                }
            });
        }
    }
}
