package forge.game.ability.effects;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

// "Choose an artist" - the picker offers every artist in the card database so a name never has
// to be recalled or spelled out.
public class ChooseArtistEffect extends SpellAbilityEffect {

    private static final List<ZoneType> VISIBLE_ZONES = ZoneType.listValueOf(
            "Battlefield,Graveyard,Exile,Stack,Command");
    private static final List<ZoneType> HIDDEN_OWN_ZONES = ZoneType.listValueOf(
            "Hand,Library,Sideboard");

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return Lang.joinHomogenous(getTargetPlayers(sa)) + " chooses an artist.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            // every artist Magic has ever printed, the way dev mode's Add a Card offers every card.
            // The cards actually in this game are the fallback if the card database isn't loaded.
            final Set<String> artists = Sets.newTreeSet();
            final StaticData data = StaticData.instance();
            if (data != null && data.getCommonCards() != null) {
                data.getCommonCards().streamAllCards().forEach(pc -> addArtist(artists, pc.getArtist()));
            }
            if (artists.isEmpty()) {
                for (final Card c : game.getCardsIn(VISIBLE_ZONES)) {
                    addArtist(artists, c.getArtist());
                }
                for (final Card c : p.getCardsIn(HIDDEN_OWN_ZONES)) {
                    addArtist(artists, c.getArtist());
                }
            }
            // Persecute Artist won't let you name its own illustrator
            if (sa.hasParam("ExcludeArtist")) {
                for (final String excluded : sa.getParam("ExcludeArtist").split(",")) {
                    artists.remove(excluded.trim());
                }
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

    private static void addArtist(final Set<String> artists, final String artist) {
        if (artist != null && !artist.isEmpty()) {
            artists.add(artist);
        }
    }
}
