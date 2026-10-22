package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Soul Drainer: the commander damage each defined commander has dealt to DamagedPlayer$ is removed. Nobody gains life;
 * it just counts as zero from now on.
 */
public class ResetCommanderDamageEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Remove all commander damage " + getDefinedCardsOrTargeted(sa) + " dealt.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParamOrDefault("DamagedPlayer", "You"), sa)) {
            for (final Card c : getDefinedCardsOrTargeted(sa)) {
                p.removeCommanderDamage(c.getGame().getCardState(c));
                p.removeCommanderDamage(c);
            }
        }
    }
}
