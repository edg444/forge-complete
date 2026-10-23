package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * You Compleat Me: "If your life total is greater than N, it becomes N. For the rest of the game, your maximum life
 * total is N." Gains past it are cut short, whatever their source (rulings).
 */
public class MaxLifeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final int max = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (p.getLife() > max) {
                p.setLife(max, sa);
            }
            p.setMaxLifeTotal(max);
        }
    }
}
