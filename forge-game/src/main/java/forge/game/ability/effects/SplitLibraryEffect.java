package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Libraries;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Split Screen: "shuffle your library and deal it into four libraries" ({@code Amount$}), or with {@code Merge$ True}
 * "shuffle your libraries together". See {@link Libraries}.
 */
public class SplitLibraryEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (sa.hasParam("Merge")) {
                Libraries.merge(p, sa);
            } else {
                Libraries.split(p, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "4"), sa), sa);
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            sb.append(p).append(sa.hasParam("Merge") ? " shuffles their libraries together. "
                    : " shuffles their library and deals it into " + sa.getParamOrDefault("Amount", "4") + " libraries. ");
        }
        return sb.toString().trim();
    }
}
