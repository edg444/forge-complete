package forge.game.ability.effects;

import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
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
        if (sa.hasParam("StackValid")) {
            // Red Herring: or a spell on the stack
            inPlay.addAll(CardLists.getValidCards(game.getCardsIn(ZoneType.Stack), sa.getParam("StackValid"), you, host, sa));
        }
        if (inHand.isEmpty() || inPlay.isEmpty()) {
            return;
        }
        final Card fromHand = you.getController().chooseSingleEntityForEffect(inHand, sa, "Choose a card in your hand to switch", null);
        final Card onField = you.getController().chooseSingleEntityForEffect(inPlay, sa,
                sa.hasParam("StackValid") ? "Choose a permanent or spell to exchange it with" : "Choose a permanent to switch it with", null);
        if (fromHand == null || onField == null) {
            return;
        }
        if (onField.isInZone(ZoneType.Stack)) {
            exchangeWithSpell(sa, fromHand, onField);
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

    /**
     * Red Herring and a spell you control: the stack object stays where it is and becomes the hand card's own spell, and
     * whatever targeted the old spell targets the new one instead, if it legally can (its ruling: otherwise it keeps
     * targeting the old spell, which is gone).
     */
    private static void exchangeWithSpell(final SpellAbility sa, final Card fromHand, final Card onStack) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        SpellAbilityStackInstance si = null;
        for (final SpellAbilityStackInstance s : game.getStack()) {
            if (s.isSpell() && s.getSourceCard() == onStack) {
                si = s;
                break;
            }
        }
        if (si == null) {
            return;
        }
        final SpellAbility oldSa = si.getSpellAbility();
        final String before = onStack.toString();
        final Player handOwner = fromHand.getOwner();
        final Player stackOwner = onStack.getOwner();

        CardCopyService.swapPrintedCards(fromHand, onStack);
        onStack.setOwner(handOwner);
        fromHand.setOwner(stackOwner);
        if (fromHand.getController() != you) {
            fromHand.setController(you, game.getNextTimestamp());
        }

        final SpellAbility newSa = onStack.getFirstSpellAbility();
        if (newSa == null) {
            return;
        }
        newSa.setActivatingPlayer(you);
        if (game.getStack().replaceInPlace(si, newSa) == null) {
            return;
        }

        for (final SpellAbilityStackInstance other : game.getStack()) {
            final TargetChoices oldTC = other.getTargetChoices().clone();
            boolean changed = false;
            for (SpellAbility part = other.getSpellAbility(); part != null; part = part.getSubAbility()) {
                if (part.usesTargeting() && part.getTargets().contains(oldSa) && part.canTargetSpellAbility(newSa)) {
                    final Integer divided = part.isDividedAsYouChoose() ? part.getTargets().getDividedValue(oldSa) : null;
                    part.getTargets().remove(oldSa);
                    part.getTargets().add(newSa);
                    if (divided != null) {
                        part.getTargets().addDividedAllocation(newSa, divided);
                    }
                    changed = true;
                }
            }
            if (changed) {
                other.updateTarget(oldTC, Sets.newHashSet());
            }
        }

        game.getAction().checkStaticAbilities();
        game.getTriggerHandler().resetActiveTriggers();
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " exchanged " + before + " on the stack with " + onStack
                + " from their hand.");
    }
}
