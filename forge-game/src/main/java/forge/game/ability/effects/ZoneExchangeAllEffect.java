package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
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

        final CardCollection list1 = new CardCollection(player1.getCardsIn(zone));
        final CardCollection list2 = new CardCollection(player2.getCardsIn(zone));

        for (final Card c : list1) {
            player2.changeOwnership(c);
            game.getAction().moveTo(zone, c, sa, AbilityKey.newMap());
        }
        for (final Card c : list2) {
            player1.changeOwnership(c);
            game.getAction().moveTo(zone, c, sa, AbilityKey.newMap());
        }

        if (zone == ZoneType.Library) {
            player1.shuffle(sa);
            player2.shuffle(sa);
        }
    }
}
