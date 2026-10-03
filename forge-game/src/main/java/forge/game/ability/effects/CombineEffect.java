package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

/**
 * Grusilda, Monster Masher: "Put two target creature cards from graveyards onto the battlefield combined into one
 * creature under your control." The pair is merged while both are still in the graveyard - the way meld readies its
 * card before moving it - so the combined creature is what enters, and both cards' enter abilities see it.
 * <p>
 * A host and a card with augment combine the augment way (rulings). Anything else keeps everything of both
 * (CardFactory.getCombinedCloneStates); a card with augment combined with a non-host so has augment without a host,
 * and the state-based action in GameAction puts both cards into the graveyard (ruling).
 */
public class CombineEffect extends SpellAbilityEffect {

    private static boolean isHost(final Card c) {
        return c.getOriginalState(CardStateName.Original).getType().hasSupertype(CardType.Supertype.Host);
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player controller = sa.getActivatingPlayer();
        final Game game = controller.getGame();
        final CardCollection cards = new CardCollection();
        for (final Card t : getTargetCards(sa)) {
            final Card c = game.getCardState(t);
            if (c != null && c.equalsWithGameTimestamp(t) && c.isInZone(ZoneType.Graveyard)) {
                cards.add(c);
            }
        }
        if (cards.isEmpty()) {
            return;
        }

        Card base = cards.getFirst();
        if (cards.size() > 1) {
            Card other = cards.get(1);
            // the augment always goes on top of a host; with a non-host it stays underneath, so the result is the
            // whole-card combination that still has augment
            if (base.isAugmentCard() != other.isAugmentCard()) {
                final Card augment = base.isAugmentCard() ? base : other;
                final Card partner = augment == base ? other : base;
                base = isHost(partner) ? partner : augment;
                other = base == partner ? augment : partner;
            }
            final boolean augmenting = other.isAugmentCard() && isHost(base);

            other.setMergedToCard(base);
            base.addMergedCard(base);
            base.addMergedCardToTop(other);
            base.setCombinedWhole(!augmenting);

            final long ts = game.getNextTimestamp();
            base.setMutatedTimestamp(ts);
            base.addCloneState(CardFactory.getMutatedCloneStates(base, sa), ts);
            game.getAction().moveTo(other.getOwner().getZone(ZoneType.Merged), other, sa);
        }

        final Card moved = game.getAction().moveToPlay(base, controller, sa, null);
        moved.updateStateForView();
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Put " + Lang.joinHomogenous(getTargetCards(sa)) + " onto the battlefield combined into one creature.";
    }
}
