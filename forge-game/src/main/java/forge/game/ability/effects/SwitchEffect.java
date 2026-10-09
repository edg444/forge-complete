package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mary O'Kill: "Switch a Killbot or Mary O'Kill in your hand with one on the battlefield."
 * <p>
 * Rulings and the Unstable FAQ: the switched-in card takes the original's place - whatever it's doing, its state, what's
 * attached to it, its counters all stay the same, as if it had been that card all along. So the permanent object stays
 * put and the two objects trade printed cards (CardCopyService.swapPrintedCards): nothing enters or leaves anything.
 * "One on the battlefield" is any player's (user, 2026-10-09); that permanent keeps its controller, and the card that
 * leaves it goes to your hand still owned by its owner, held there as Five-Finger Discount holds a card.
 */
public class SwitchEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final Card host = sa.getHostCard();

        final CardCollection inHand = CardLists.getValidCards(you.getCardsIn(ZoneType.Hand), sa.getParam("HandValid"), you, host, sa);
        final CardCollection inPlay = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("BattlefieldValid"), you, host, sa);
        if (inHand.isEmpty() || inPlay.isEmpty()) {
            return;
        }
        final Card fromHand = you.getController().chooseSingleEntityForEffect(inHand, sa, "Choose a card in your hand to switch", null);
        final Card onField = you.getController().chooseSingleEntityForEffect(inPlay, sa, "Choose a permanent to switch it with", null);
        if (fromHand == null || onField == null) {
            return;
        }

        final Player handOwner = fromHand.getOwner();
        final Player fieldOwner = onField.getOwner();
        final Player fieldController = onField.getController();
        final String before = onField.toString();

        CardCopyService.swapPrintedCards(fromHand, onField);
        onField.setOwner(handOwner);
        fromHand.setOwner(fieldOwner);
        // the permanent's controller is part of the state that stays the same; the card in your hand is held by you
        if (onField.getController() != fieldController) {
            onField.setController(fieldController, game.getNextTimestamp());
        }
        if (fromHand.getController() != you) {
            fromHand.setController(you, game.getNextTimestamp());
        }

        game.getAction().checkStaticAbilities();
        game.getTriggerHandler().resetActiveTriggers();
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " switched " + before + " on the battlefield with " + onField
                + " from their hand.");
    }
}
