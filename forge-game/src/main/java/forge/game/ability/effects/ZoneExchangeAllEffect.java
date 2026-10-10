package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

// Mirror Mirror: "...exchange cards in your hands, cards in your libraries, and cards in your
// graveyards." The existing ExchangeZone (ZoneExchangeEffect) only swaps one chosen card between
// two zones (built for Aura-style effects) - nothing swaps an entire zone's contents between two
// players wholesale. Reused three times via Zone$ (Hand/Library/Graveyard). Hand/library/graveyard
// membership is owner-keyed, not controller-keyed, so this needs a genuine ownership change per
// card (Player.changeOwnership, the same primitive the real Ante cards Tempest Efreet/Bronze
// Tablet use via GainOwnership+ChangeZone) followed by physically relocating it - done directly in
// Java since looping that two-step DSL combo per card across a whole library would be untenable.
public class ZoneExchangeAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }
        return "Exchange " + sa.getParamOrDefault("Zone", "Hand") + " contents.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<Player> players = getDefinedPlayersOrTargeted(sa);
        if (players.size() != 2) {
            return;
        }
        final Player player1 = players.get(0);
        final Player player2 = players.get(1);
        final Game game = player1.getGame();
        final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Hand"));

        // The physical zone, not getCardsIn: that one answers for whoever holds the graveyard
        // (Graveyard Busybody) and isn't in pile order.
        final CardCollection list1 = new CardCollection(player1.getZone(zone).getCards());
        final CardCollection list2 = new CardCollection(player2.getZone(zone).getCards());

        // Each pile slides across as-is (Everythingamajig/Mirror Mirror rulings: "don't change the
        // order"), so every card goes to the far end of its new zone in its old order. The other
        // player's cards are still in that zone ahead of them while this runs and leave next.
        moveAcross(game, list1, player2, zone, sa);
        moveAcross(game, list2, player1, zone, sa);
    }

    private static void moveAcross(final Game game, final CardCollection cards, final Player newOwner,
            final ZoneType zone, final SpellAbility sa) {
        for (final Card c : cards) {
            newOwner.changeOwnership(c);
            final Zone to = newOwner.getZone(zone);
            // a hand has no pile order, and leaving its position open lets a programmed hand
            // (The Grand Calcutron) place the card
            final Integer position = zone == ZoneType.Hand ? null : to.size();
            game.getAction().changeZone(game.getZoneOf(c), to, c, position, sa);
        }
    }
}
