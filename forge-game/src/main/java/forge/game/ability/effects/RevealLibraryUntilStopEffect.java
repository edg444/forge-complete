package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Push Your Luck: reveal cards from the top of your library one at a time until you decide to stop - at least one, if
 * there are any (rulings). They stay where they are. The host's RevealedMV SVar is their total mana value, X being 0.
 */
public class RevealLibraryUntilStopEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " reveals cards from the top of their library until they decide to stop.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final CardCollection revealed = new CardCollection();
        int total = 0;
        host.setSVar("RevealedMV", "Number$0");
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            if (!revealed.isEmpty() && !p.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose,
                    "The cards revealed so far have a total mana value of " + total + ". Reveal another?", null)) {
                break;
            }
            revealed.add(c);
            total += c.getCMC();
            host.setSVar("RevealedMV", "Number$" + total);
            p.getGame().getAction().reveal(new CardCollection(c), p, true,
                    "Revealed (total mana value " + total + "): ");
        }
    }
}
