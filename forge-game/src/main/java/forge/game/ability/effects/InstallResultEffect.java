package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Socketed Sprocketer: a die result "installed" on a permanent is the die itself sitting on it, kept for later.
 * Uninstall$ All takes every result off; Install$ puts a result on. A result on it can later stand in for a die
 * its controller rolls (see RollDiceEffect), or pay an Uninstall&lt;N&gt; cost.
 */
public class InstallResultEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card card = c.getGame().getCardState(c);
            if (!card.isInPlay()) {
                continue;
            }
            if ("All".equals(sa.getParam("Uninstall"))) {
                card.clearInstalledResults();
            }
            if (sa.hasParam("Install")) {
                card.installResult(AbilityUtils.calculateAmount(host, sa.getParam("Install"), sa));
            }
        }
    }
}
