package forge.game.ability.effects;

import java.util.ArrayList;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.event.GameEventFlipOntoBattlefield;
import forge.game.event.TossStyle;
import forge.game.spellability.SpellAbility;

/**
 * A card thrown up and coming back down on its own spot, touching nothing: Orcish Paratroopers' drop, Clay Pigeon's toss.
 * Only shows the throw (the throw animation, in place); what the throw decides is up to the rest of the script.
 * Flips$ is how many times it turns over; LandFaceDown$ True ends it upside down.
 */
public class TossCardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int flips = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Flips", "1"), sa);
        final TossStyle style = TossStyle.inPlace(sa.hasParam("LandFaceDown"));
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final CardView view = c.getView();
            host.getGame().fireEvent(new GameEventFlipOntoBattlefield(view, view, new ArrayList<>(), flips, false, style));
            host.getGame().fireEvent(new GameEventFlipOntoBattlefield(view, view, new ArrayList<>(), flips, true, style));
        }
    }
}
