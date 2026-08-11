package forge.game.ability.effects;

import java.util.Map;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ItemPool;

/**
 * Present Arms - "Exchange your library with another deck you own from outside the game."
 * <p>
 * The deck is a real saved deck, offered by the controller (only the human side can see a
 * collection; the AI declines and nothing happens). The library that gets displaced goes to the
 * sideboard, which is how Forge represents "outside the game" everywhere else - wishes pull from
 * there - and is what leaves the original deck somewhere Decorated Knight can still draw from.
 */
public class SwapLibraryWithDeckEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - exchange library with a deck from outside the game.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final Deck deck = p.getController().chooseDeckFromCollection(
                    "Choose a deck you own from outside the game");
            if (deck == null || deck.getMain() == null || deck.getMain().isEmpty()) {
                continue;
            }

            // the outgoing library steps outside the game before the new one arrives
            final CardCollectionView outgoing = new CardCollection(p.getCardsIn(ZoneType.Library));
            for (final Card c : outgoing) {
                final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                final Card moved = game.getAction().moveTo(ZoneType.Sideboard, c, sa, moveParams);
                if (moved != null) {
                    // tag the card that actually landed in the sideboard, not the one that left the
                    // library - the move hands the destination zone a copy
                    moved.setDisplacedFromLibrary(true);
                }
            }

            for (final Map.Entry<PaperCard, Integer> entry : ItemPool.createFrom(deck.getMain(),
                    PaperCard.class)) {
                for (int i = 0; i < entry.getValue(); i++) {
                    final Card card = Card.fromPaperCard(entry.getKey(), p);
                    final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                    game.getAction().moveTo(ZoneType.Library, card, sa, moveParams);
                }
            }

            p.shuffle(sa);
        }
    }
}
