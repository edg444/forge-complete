package forge.game.ability.effects;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.util.Lang;

// Circle of Protection: Art: "As this enchantment enters, choose an artist." The full list of every
// artist Magic has ever printed is far too long to pick from in a dialog, so the choices are the
// artists actually represented among cards visible in this game - which is also the only set of
// artists the choice can ever matter for, since the prevention only applies to damage sources here.
public class ChooseArtistEffect extends SpellAbilityEffect {

    private static final List<ZoneType> VISIBLE_ZONES = ZoneType.listValueOf(
            "Battlefield,Graveyard,Exile,Stack,Command");

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return Lang.joinHomogenous(getTargetPlayers(sa)) + " chooses an artist.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final Set<String> artists = Sets.newTreeSet();
            for (final Card c : game.getCardsIn(VISIBLE_ZONES)) {
                addArtist(artists, c);
            }
            // a player always knows their own hidden cards, and the enchantment is theirs to aim
            for (final Card c : p.getCardsIn(ZoneType.Hand)) {
                addArtist(artists, c);
            }
            if (artists.isEmpty()) {
                continue;
            }

            final String chosen = p.getController().chooseSomeType("Artist", sa, artists);
            if (chosen == null) {
                continue;
            }
            host.setChosenArtist(chosen);
            game.getAction().notifyOfValue(sa, host, chosen, p);
        }
    }

    private static void addArtist(final Set<String> artists, final Card c) {
        final IPaperCard pc = c.getPaperCard();
        if (pc != null && !pc.getArtist().isEmpty()) {
            artists.add(pc.getArtist());
        }
    }
}
