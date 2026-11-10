package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.MyRandom;

/**
 * Draft an ability (Enter the Eternities): Offered$ random abilities from the Abilities$ pool (SVar names, default
 * three) are shown and the player keeps one, written into the host's ResultSVar$ for a later TokenAbilities$.
 * Same API name as upstream's open PR #12584 so the two can be reconciled. Fork version.
 */
public class DraftAbilityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getActivatingPlayer() + " drafts an ability.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final List<String> pool = new ArrayList<>();
        for (final String name : sa.getParam("Abilities").split(",")) {
            pool.add(name.trim());
        }
        Collections.shuffle(pool, MyRandom.getRandom());
        final int offered = Math.min(pool.size(), AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Offered", "3"), sa));
        if (offered <= 0) {
            return;
        }
        final List<String> names = pool.subList(0, offered);
        final List<SpellAbility> options = new ArrayList<>();
        for (final String name : names) {
            final SpellAbility option = AbilityFactory.getAbility(host, name);
            option.setActivatingPlayer(player);
            options.add(option);
        }
        final SpellAbility chosen = player.getController().chooseSingleSpellForEffect(options, sa,
                Localizer.getInstance().getMessage("lblChooseAbilityToDraft"), Map.<String, Object>of("DraftedFrom", names));
        final int index = Math.max(0, options.indexOf(chosen));
        final String chosenName = names.get(index);
        host.setSVar(sa.getParamOrDefault("ResultSVar", "DraftedAbility"), host.getSVar(chosenName));
        host.getGame().getAction().notifyOfValue(sa, host, options.get(index).getDescription(), player);
    }
}
