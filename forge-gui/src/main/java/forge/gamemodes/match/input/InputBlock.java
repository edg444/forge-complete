/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.match.input;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventCombatUpdate;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.gui.events.UiEventBlockerAssigned;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * Input_Block class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputBlock.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputBlock extends InputSyncronizedBase {

    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    // some cards may block several creatures at a time. (ex:  Two-Headed Dragon, Vanguard's Shield)
    private final Combat combat;
    private final Player defender;
    // Judge Tower: the blocks that couldn't be declared as they were picked (blocker, attacker), for the judge
    private final List<Pair<Card, Card>> illegalBlocks = new ArrayList<>();

    public InputBlock(final PlayerControllerHuman controller, final Player defender0, final Combat combat0) {
        super(controller);
        defender = defender0;
        combat = combat0;

        //auto-select first attacker to declare blockers for
        if (isJudged()) {
            // which one that is mustn't say anything about what can block
            currentAttacker = combat.getAttackers().stream().filter(a -> a.getController() != defender).findFirst().orElse(null);
            return;
        }
        for (final Card attacker : combat.getAttackers()) {
            for (final Card c : defender.getCreaturesInPlay()) {
                if (CombatUtil.canBlock(attacker, c, combat)) {
                    currentAttacker = attacker;
                    return;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        // could add "Reset Blockers" button
        Localizer localizer = Localizer.getInstance();
        getController().getGui().updateButtons(getOwner(), true, false, true);

        if (currentAttacker == null) {
            showMessage(localizer.getMessage("lblSelectBlockTarget"));
        } else {
            String attackerName = currentAttacker.isFaceDown() ? localizer.getMessage("lblMorph") : currentAttacker.getDisplayName() + " (" + currentAttacker.getId() + ")";
            String message = localizer.getMessage("lblSelectBlocker") + attackerName + " " + localizer.getMessage("lblOrSelectBlockTarget");
            showMessage(message);
        }

        if (combat != null)
            getController().getGame().fireEvent(GameEventCombatUpdate.fromCards(combat.getAttackers(), combat.getAllBlockers()));

        getController().getGui().showCombat();
    }

    @Override
    protected boolean allowAwaitNextInput() {
        return true;
    }

    @Override
    protected List<CardView> getActionable() {
        return getController().getBlockerCandidates(defender, combat);
    }

    /** {@inheritDoc} */
    @Override
    public final void onOk() {
        final String blockErrors = CombatUtil.validateBlocks(combat, defender);
        // Judge Tower: the judge rules on an illegal block, it isn't refused
        if (blockErrors == null || isJudged()) {
            // Done blocking
            stop();
        } else {
            //must run in game thread to prevent problems for mobile game
            ThreadUtil.invokeInGameThread(() -> getController().getGui().message(blockErrors));
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        boolean isCorrectAction = false;
        if (triggerEvent != null && triggerEvent.getButton() == 3 && card.getController() == defender) {
            unblock(card);
            card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(card), null));
            if (otherCardsToSelect != null) {
                for (Card c : otherCardsToSelect) {
                    if (c.getController() == defender) {
                        unblock(c);
                        c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(c), null));
                    }
                }
            }
            isCorrectAction = true;
        } else if (isAttackerToBlock(card)) {
            currentAttacker = card;
            isCorrectAction = true;
        } else if (currentAttacker != null && mayPick(card)) {
            // card is valid to even be a blocker
            if (combat.isBlocking(card, currentAttacker)) {
                //if creature already blocking current attacker, remove blocker from combat
                combat.removeBlockAssignment(currentAttacker, card);
                illegalBlocks.remove(Pair.of(card, currentAttacker));
                card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(card), null));
                if (otherCardsToSelect != null) {
                    for (Card c : otherCardsToSelect) {
                        if (combat.isBlocking(c, currentAttacker)) {
                            combat.removeBlockAssignment(currentAttacker, c);
                            illegalBlocks.remove(Pair.of(c, currentAttacker));
                            c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(c), null));
                        }
                    }
                }
                isCorrectAction = true;
            } else {
                isCorrectAction = mayBlock(card);
                if (isCorrectAction) {
                    combat.addBlocker(currentAttacker, card);
                    card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(
                            CardView.get(card), CardView.get(currentAttacker)));
                    if (otherCardsToSelect != null) {
                        for (Card c : otherCardsToSelect) {
                            if (mayBlock(c)) {
                                combat.addBlocker(currentAttacker, c);
                                c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(
                                        CardView.get(c), CardView.get(currentAttacker)));
                            }
                        }
                    }
                }
            }
        }

        if (isCorrectAction) {
            card.getGame().fireEvent(new GameEventCombatChanged());
        }

        showSelection();
        showMessage();
        return isCorrectAction;
    }

    private boolean mayPick(final Card card) {
        return isJudged() ? card.isInPlay() : card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card);
    }

    private boolean mayBlock(final Card card) {
        if (!isJudged()) {
            return CombatUtil.canBlock(currentAttacker, card, combat);
        }
        if (!isLegalBlock(card, currentAttacker)) {
            illegalBlocks.add(Pair.of(card, currentAttacker));
        }
        return true;
    }

    private boolean isLegalBlock(final Card blocker, final Card attacker) {
        return defender.getZone(ZoneType.Battlefield).contains(blocker) && CombatUtil.canBlock(attacker, blocker, combat);
    }

    @Override
    public void showAndWait() {
        super.showAndWait();
        for (final Pair<Card, Card> block : illegalBlocks) {
            // taking another block back since then can have made this one fine
            combat.removeBlockAssignment(block.getRight(), block.getLeft());
            final boolean legal = isLegalBlock(block.getLeft(), block.getRight());
            combat.addBlocker(block.getRight(), block.getLeft());
            if (!legal) {
                getJudge().illegalBlock(getController().getPlayer(), block.getLeft(), block.getRight());
            }
        }
    }

    /**
     * Party Crasher: during an opponent's turn their attackers and yours share a combat, and an untapped creature of
     * yours that's attacking can still block - so only other players' attackers are picked out to be blocked.
     */
    private boolean isAttackerToBlock(final Card card) {
        return combat.isAttacking(card) && card.getController() != defender;
    }

    // undoes only the block of a creature that's attacking as well
    private void unblock(final Card card) {
        illegalBlocks.removeIf(block -> block.getLeft().equals(card));
        if (combat.isAttacking(card)) {
            combat.undoBlockingAssignment(card);
        } else {
            combat.removeFromCombat(card);
        }
    }

    @Override
    public String getActivateAction(Card card) {
        if (isAttackerToBlock(card)) {
            return Localizer.getInstance().getMessage("lblDeclareBlockersForCard");
        }
        if (currentAttacker != null && mayPick(card)) {
            if (combat.isBlocking(card, currentAttacker)) {
                return Localizer.getInstance().getMessage("lblRemoveFromCombat");
            }
            if (isJudged() || CombatUtil.canBlock(currentAttacker, card, combat)) {
                return Localizer.getInstance().getMessage("lblBlockWithCard");
            }
        }
        return null;
    }

    @Override
    protected Selection getSelection() {
        return currentAttacker == null ? Selection.NONE : Selection.ofChosen(List.of(CardView.get(currentAttacker)));
    }
}
