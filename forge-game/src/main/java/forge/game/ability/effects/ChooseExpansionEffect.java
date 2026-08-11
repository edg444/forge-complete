package forge.game.ability.effects;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.StaticData;
import forge.card.CardEdition;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * World-Bottling Kit: "choose a Magic set". Every set is offered, the way the artist picker offers
 * every artist. The choice is kept on the card so a valid string can match against it.
 */
public class ChooseExpansionEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - choose a Magic set.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            // Stocking Tiger has to actually open the thing, so it asks for sets that can produce a
            // booster; World-Bottling Kit bottles any set at all and takes the unfiltered list.
            final boolean needsBooster = sa.hasParam("WithBoosters");
            final Set<String> sets = Sets.newTreeSet();
            final StaticData data = StaticData.instance();
            if (data != null && data.getEditions() != null) {
                for (final CardEdition e : data.getEditions().getOrderedEditions()) {
                    if (!e.getCode().isEmpty() && (!needsBooster || e.getBoosterTemplate() != null)) {
                        sets.add(e.getCode());
                    }
                }
            }
            if (sets.isEmpty()) {
                continue;
            }
            final String chosen = p.getController().chooseSomeType("Magic set", sa, List.copyOf(sets));
            if (chosen == null) {
                continue;
            }
            host.setChosenExpansion(chosen);
            host.getGame().getAction().notifyOfValue(sa, host, chosen, p);
        }
    }
}
