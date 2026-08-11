package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.List;

/**
 * The dexterity half of Goblin Sleigh Ride: a creature rides the card across the table, and what it
 * hits on the way is a physical outcome rather than a target.
 * <p>
 * Modelled on {@link FlipOntoBattlefieldEffect} - you say where you were aiming, and chance decides
 * the rest - because that is already how Forge stands in for Chaos Orb and Falling Star, and the
 * table's answer to "did it stay on?" isn't something one player should simply declare. Cards
 * touched are remembered on the host for a following damage effect to use.
 */
public class SlideOntoBattlefieldEffect extends FlipOntoBattlefieldEffect {
    private static final float CHANCE_TO_HIT = 0.70f;
    private static final float CHANCE_TO_HIT_TWO_CARDS = 0.20f;

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = host.getGame();

        final int stayPercent = sa.hasParam("StayPercent")
                ? AbilityUtils.calculateAmount(host, sa.getParam("StayPercent"), sa) : 70;
        final String validTouched = sa.getParamOrDefault("ValidTouched", "Creature");

        // The rider is along for the ride, so it can't be something the sleigh runs into.
        final List<Card> riders = sa.hasParam("Rider")
                ? AbilityUtils.getDefinedCards(host, sa.getParam("Rider"), sa)
                : new CardCollection();

        CardCollection eligible = new CardCollection(CardLists.getValidCards(
                game.getCardsIn(ZoneType.Battlefield), validTouched, p, host, sa));
        eligible.removeAll(riders);
        if (eligible.isEmpty()) {
            game.getAction().notifyOfValue(sa, host, "There was nothing in the sleigh's path.", null);
            return;
        }

        CardCollectionView aimedAt = p.getController().chooseCardsForEffect(eligible, sa,
                "Aim the sleigh - which creature were you sliding toward?", 1, 1, false, null);
        if (aimedAt.isEmpty()) {
            return;
        }
        final Card target = aimedAt.getFirst();

        if (!MyRandom.percentTrue(stayPercent)) {
            game.getAction().notifyOfValue(sa, host, "The creature fell off the sleigh.", null);
            return;
        }
        game.getAction().notifyOfValue(sa, host, "The creature stayed on!", null);

        CardCollection randChoices = new CardCollection();
        randChoices.add(target);
        Card lhsNeighbor = getNeighboringCard(target, -1);
        Card rhsNeighbor = getNeighboringCard(target, 1);
        for (Card neighbor : new Card[] {lhsNeighbor, rhsNeighbor}) {
            if (neighbor != null && !randChoices.contains(neighbor) && eligible.contains(neighbor)) {
                randChoices.add(neighbor);
                break;
            }
        }

        CardCollection hit = new CardCollection();
        float outcome = MyRandom.getRandom().nextFloat();
        if (outcome <= CHANCE_TO_HIT_TWO_CARDS) {
            hit.addAll(Aggregates.random(randChoices, randChoices.size() > 1 ? 2 : 1));
        } else if (outcome <= CHANCE_TO_HIT) {
            hit.add(Aggregates.random(randChoices));
        }

        if (hit.isEmpty()) {
            game.getAction().notifyOfValue(sa, host, "The sleigh slid past without touching anything.", null);
        } else {
            game.getAction().notifyOfValue(sa, host, "The sleigh touched " + hit + ".", null);
        }

        host.addRemembered(hit);
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - slide the sleigh across the battlefield.";
    }
}
