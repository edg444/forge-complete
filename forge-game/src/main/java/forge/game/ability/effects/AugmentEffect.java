package forge.game.ability.effects;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unstable's augment: combine the augment card, still in its owner's hand, with the target host. The combined
 * creature is the host carrying on - same object, so it keeps its counters, damage and summoning sickness (or
 * lack of it) - which is exactly what mutate's merge plumbing does, so this reuses it; only the resulting
 * characteristics differ (see CardFactory.getAugmentedCloneStates).
 * <p>
 * With {@code ChangeType$}, the augment card is instead searched for in the activator's library (Teacher's Pet).
 */
public class AugmentEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card target = getTargetCards(sa).getFirst();
        if (target == null || !target.isInPlay() || !target.getType().hasSupertype(CardType.Supertype.Host)) {
            return;
        }

        final Card augment;
        if (sa.hasParam("ChangeType")) {
            // a search for a card with a stated quality, so the searcher may fail to find one
            final Player p = sa.getActivatingPlayer();
            final CardCollection choices = CardLists.getValidCards(p.getCardsIn(ZoneType.Library),
                    sa.getParam("ChangeType"), p, sa.getHostCard(), sa);
            augment = p.getController().chooseSingleEntityForEffect(choices, sa,
                    "Choose " + sa.getParamOrDefault("ChangeTypeDesc", "a card"), true, null);
            if (augment == null) {
                return;
            }
        } else {
            augment = sa.getHostCard();
            // ruling: if the card with augment has left your hand, nothing happens
            if (!augment.isInZone(ZoneType.Hand)) {
                return;
            }
        }
        final Game game = augment.getGame();

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
        if (sa.hasParam("ChangeType")) {
            return "Search for " + sa.getParamOrDefault("ChangeTypeDesc", "a card") + " and combine it with "
                    + getTargetCards(sa).getFirst() + ".";
        }
        return "Combine " + sa.getHostCard() + " with " + getTargetCards(sa).getFirst() + ".";
    }
}
