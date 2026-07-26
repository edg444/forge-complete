package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

// Brushstroke Paintermage: "Target permanent's artist becomes the artist of your choice until end
// of turn." The artist to use is whichever one the host card was last asked to choose.
public class SetArtistEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return Lang.joinHomogenous(getTargetCards(sa)) + "'s artist becomes "
                + sa.getHostCard().getChosenArtist() + ".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!host.hasChosenArtist()) {
            return;
        }
        final String artist = host.getChosenArtist();

        for (final Card c : getTargetCards(sa)) {
            if (!c.isInPlay() || !c.canBeTargetedBy(sa)) {
                continue;
            }
            // null restores the printed artist, which is what a card with no override had
            final String previous = c.getChangedArtist();
            c.setChangedArtist(artist);
            if (!"Permanent".equals(sa.getParam("Duration"))) {
                host.getGame().getEndOfTurn().addUntil(() -> c.setChangedArtist(previous));
            }
        }
    }
}
