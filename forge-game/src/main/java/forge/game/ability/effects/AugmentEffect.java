package forge.game.ability.effects;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unstable's augment: combine the augment card, still in its owner's hand, with the target host. The combined
 * creature is the host carrying on - same object, so it keeps its counters, damage and summoning sickness (or
 * lack of it) - which is exactly what mutate's merge plumbing does, so this reuses it; only the resulting
 * characteristics differ (see CardFactory.getAugmentedCloneStates).
 */
public class AugmentEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card augment = sa.getHostCard();
        final Game game = augment.getGame();

        // ruling: if the card with augment has left your hand, nothing happens
        if (!augment.isInZone(ZoneType.Hand)) {
            return;
        }
        final Card target = getTargetCards(sa).getFirst();
        if (target == null || !target.isInPlay() || !target.getType().hasSupertype(CardType.Supertype.Host)) {
            return;
        }

        augment.setMergedToCard(target);
        if (!target.hasMergedCard()) {
            target.addMergedCard(target);
        }
        target.addMergedCardToTop(augment);

        target.removeMutatedStates();
        final long ts = game.getNextTimestamp();
        target.setMutatedTimestamp(ts);
        target.addCloneState(CardFactory.getMutatedCloneStates(target, sa), ts);

        game.getTriggerHandler().clearActiveTriggers(target, null);
        game.getTriggerHandler().registerActiveTrigger(target, false);

        game.getAction().moveTo(augment.getOwner().getZone(ZoneType.Merged), augment, sa);

        augment.setTapped(target.isTapped());
        target.updateStateForView();
        target.updateTokenView();
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Combine " + sa.getHostCard() + " with " + getTargetCards(sa).getFirst() + ".";
    }
}
