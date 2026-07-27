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

// Greater Morphling: "this creature's expansion symbol becomes the symbol of your choice". Every
// set is offered, the way the artist picker offers every artist. The card keeps being the printing
// it always was - only the symbol shown changes - so this is separate from its actual set code.
public class SetExpansionEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + "'s expansion symbol becomes the symbol of your choice.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final Set<String> sets = Sets.newTreeSet();
            final StaticData data = StaticData.instance();
            if (data != null && data.getEditions() != null) {
                for (final CardEdition e : data.getEditions().getOrderedEditions()) {
                    if (!e.getCode().isEmpty()) {
                        sets.add(e.getCode());
                    }
                }
            }
            if (sets.isEmpty()) {
                continue;
            }

            final String chosen = p.getController().chooseSomeType("Expansion symbol", sa,
                    List.copyOf(sets));
            if (chosen == null) {
                continue;
            }

            final String previous = host.getChangedSetCode();
            host.setChangedSetCode(chosen);
            if (!"Permanent".equals(sa.getParam("Duration"))) {
                host.getGame().getEndOfTurn().addUntil(() -> host.setChangedSetCode(previous));
            }
            host.getGame().getAction().notifyOfValue(sa, host, chosen, p);
        }
    }
}
