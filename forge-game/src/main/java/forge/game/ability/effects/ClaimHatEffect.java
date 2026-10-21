package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * T.A.P.P.E.R.: pointing out that one of the Choices$ permanents has a hat (honor system - only the art, or a hat sticker,
 * shows it). The claim stays with the card (Card.setHasHatClaim), and Card.HasHat counts it from then on.
 */
public class ClaimHatEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " points out a hat.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final CardCollection options = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParam("Choices"), you, sa.getHostCard(), sa);
        if (options.isEmpty()) {
            return;
        }
        final Card chosen = you.getController().chooseSingleEntityForEffect(options, sa,
                "Choose a creature that has a hat (one its art shows it with, worn or not, or a hat, wig or headband"
                        + " sticker - not a space helmet, a hood or a face mask, and not a background character's)", true, null);
        if (chosen == null) {
            return;
        }
        chosen.setHasHatClaim(true);
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " says " + chosen + " has a hat.");
    }
}
