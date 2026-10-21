package forge.game.ability.effects;

import forge.ImageKeys;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.HeadBalance;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

/**
 * Form of the Approach of the Second Sun: "You become a card ... Put yourself seventh from the top." Its activator
 * becomes a {@link GamePieceType#PERSON} card - per its rulings a planeswalker card with no other characteristics -
 * put LibraryPosition$ from the top of their library (the bottom if it's shorter). A player who's already a card is
 * just moved there. The cards now above them are on their head (see HeadBalance).
 */
public class BecomeACardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " becomes a card in their library.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final Zone library = p.getZone(ZoneType.Library);
        final int wanted = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("LibraryPosition", "0"), sa);

        Card self = HeadBalance.personCard(p);
        if (self != null) {
            library.remove(self);
        } else {
            self = createPerson(p);
        }
        final int position = Math.min(wanted, library.size());
        library.add(self, position);
        self.setZone(library);

        self.setCardsOnHead(position);
        game.getAction().notifyOfValue(sa, p, p + " is now a card, " + (position + 1) + " from the top of their library", null);
        if (p.getController().isAI()) {
            // balancing the stack in the first place is the first chance for it to fall
            HeadBalance.wobble(p, position);
        }
    }

    private static Card createPerson(final Player owner) {
        final Game game = owner.getGame();
        final Card person = new Card(game.nextCardId(), game);
        person.setGamePieceType(GamePieceType.PERSON);
        // a planeswalker card with no other characteristics (rulings): no name, mana cost, color or subtypes
        person.setName("");
        person.setOwner(owner);
        person.setType(CardType.parse("Planeswalker", false));
        person.setColor(ColorSet.C);
        person.setImageKey(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD));
        person.setGameTimestamp(game.getNextTimestamp());
        person.setLayerTimestamp(game.getNextTimestamp());
        return person;
    }
}
