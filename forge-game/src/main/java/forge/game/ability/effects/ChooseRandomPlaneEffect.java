package forge.game.ability.effects;

import java.util.List;

import forge.StaticData;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.util.Aggregates;

/**
 * Polis the Planeshifter: a plane at random from among all planechase planes (not phenomena), kept imprinted on the
 * host, which has all its abilities (StaticAbilityContinuous GainsPlaneAbilities$). The last one chosen replaces any
 * before it.
 */
public class ChooseRandomPlaneEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Choose a plane at random from among all planechase planes.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final List<PaperCard> planes = StaticData.instance().getVariantCards().getUniqueCards().stream()
                .filter(pc -> pc.getRules().getType().isPlane()).toList();
        final PaperCard plane = Aggregates.random(planes);
        if (plane == null) {
            return;
        }
        final Card chosen = Card.fromPaperCard(plane, sa.getActivatingPlayer());
        host.clearImprintedCards();
        host.addImprintedCard(chosen);
        host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, host + " has the abilities of " + chosen.getName() + ".");
        host.getGame().getAction().checkStaticAbilities();
    }
}
