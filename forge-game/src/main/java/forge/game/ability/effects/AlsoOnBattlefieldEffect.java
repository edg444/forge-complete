package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unstable's Masterful Ninja: "[this card] is on the battlefield and in your hand until end of turn." The card goes
 * onto the battlefield without anything noticing it enter, stays listed in the hand it came from, and at end of
 * turn is just a card in that hand again without anything noticing it leave.
 */
public class AlsoOnBattlefieldEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            sb.append(c).append(" is on the battlefield and in ").append(c.getOwner()).append("'s hand until end of turn. ");
        }
        return sb.toString().trim();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final Player activator = sa.getActivatingPlayer();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final Card gameCard = game.getCardState(c, null);
            // already in both zones, or it left the hand since this was activated
            if (gameCard == null || !gameCard.isInZone(ZoneType.Hand) || !gameCard.equalsWithGameTimestamp(c)) {
                continue;
            }
            final Card inPlay = game.getAction().putOntoBattlefieldAlsoInHand(gameCard, activator, sa);
            if (inPlay == null) {
                continue;
            }
            final long timestamp = inPlay.getGameTimestamp();
            game.getEndOfTurn().addUntil(() -> {
                if (GameAction.isAlsoInHand(inPlay) && inPlay.getGameTimestamp() == timestamp) {
                    game.getAction().endAlsoInHand(inPlay);
                }
            });
        }
    }
}
