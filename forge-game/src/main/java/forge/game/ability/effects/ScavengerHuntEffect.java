package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * Scavenger Hunt: one of three items is chosen at random, and you have ten seconds to search your library and reveal a
 * card with it in its art. Art Scryfall Tagger tags with the item counts at once (animal-person or alien, food, horn or
 * tusk - PrintingTraits art objects); for any other card the player reveals, they're asked (honor system, as for the
 * other art cards). The AI finds a tagged card in time four times out of five, if it has one. The host's Found SVar is 1
 * when a card was revealed. The library is shuffled after.
 */
public class ScavengerHuntEffect extends SpellAbilityEffect {

    public static final List<String> ITEMS = List.of("a humanoid animal or alien", "food", "horns and tusks");

    public static boolean knownInArt(final Card c, final String item) {
        return switch (item) {
            case "a humanoid animal or alien" -> c.hasKnownArtObject("animal-person") || c.hasKnownArtObject("alien");
            case "food" -> c.hasKnownArtObject("food");
            default -> c.hasKnownArtObject("horn") || c.hasKnownArtObject("tusk");
        };
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " hunts their library for an item in a card's art.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("Found", "Number$0");
        final String item = Aggregates.random(ITEMS);
        game.getAction().notifyOfValue(sa, you, item, null);
        final CardCollectionView library = you.getCardsIn(ZoneType.Library);
        boolean found = false;
        Card shown = null;
        if (you.getController().isAI()) {
            final CardCollection known = CardLists.filter(library, c -> knownInArt(c, item));
            if (!known.isEmpty() && MyRandom.percentTrue(80)) {
                shown = known.getFirst();
                found = true;
            }
        } else if (!library.isEmpty()) {
            shown = you.getController().chooseSingleEntityForEffect(new CardCollection(library), sa,
                    "Ten seconds! Reveal a card with " + item + " in its art", true, null);
            if (shown != null) {
                found = knownInArt(shown, item) || you.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose,
                        "Does " + shown + "'s art show " + item + "?", null);
            }
        }
        if (shown != null) {
            game.getAction().reveal(new CardCollection(shown), you, true, "Scavenger Hunt (" + item + "): ");
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + (found ? " finds " : " doesn't find ") + item + " in time.");
        if (found) {
            host.setSVar("Found", "Number$1");
        }
        you.shuffle(sa);
    }
}
