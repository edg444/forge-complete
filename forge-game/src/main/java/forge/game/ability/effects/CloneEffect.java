package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.GameCommand;
import forge.StaticData;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;
import forge.util.Localizer;
import forge.util.collect.FCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloneEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        Card tgtCard = host;

        Card cardToCopy = host;
        if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.hasParam("CopyCardName")) {
            cardToCopy = null;
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargetCard();
        }

        List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }

        sb.append(tgtCard);
        sb.append(" becomes a copy of ").append(cardToCopy == null ? sa.getParam("CopyCardName") : cardToCopy).append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        if (!checkValidDuration(sa.getParam("Duration"), sa)) {
            return;
        }

        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        List<Card> cloneTargets = new ArrayList<>();
        final Game game = activator.getGame();
        final List<String> pumpKeywords = Lists.newArrayList();

        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        // find cloning source i.e. thing to be copied
        Card cardToCopy = null;

        if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));

            // choices need to be filtered by LastState Battlefield or Graveyard
            // if a Clone enters the field as other cards it could clone,
            // the clone should not be able to clone them
            // but do that only for Replacement Effects
            CardCollectionView lastState = null;
            if (sa.isReplacementAbility()) {
                if (choiceZone.equals(ZoneType.Battlefield)) {
                    lastState = sa.getLastStateBattlefield();
                } else if (choiceZone.equals(ZoneType.Graveyard)) {
                    lastState = sa.getLastStateGraveyard();
                }
            }

            if (lastState == null) {
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);
            } else {
                // CR 614.12a what it can copy is decided by how they looked before the event
                choices.retainAll(CardLists.getValidCards(lastState, sa.getParam("Choices"), activator, host, sa));
            }
            boolean choiceOpt = sa.hasParam("ChoiceOptional");

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") :
                    Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            cardToCopy = activator.getController().chooseSingleEntityForEffect(choices, sa, title, choiceOpt, null);
        } else if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.hasParam("CopyCardName")) {
            cardToCopy = StaticAbilityCastAs.template(game, sa.getParam("CopyCardName"), activator);
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargetCard();
        } else if (sa.hasParam("CopyFromChosenName")) {
            String name = host.getNamedCard();
            cardToCopy = Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(name), activator);
        }
        // each target copies its own copiable values, with the changes this makes (Bombardment's Missiles)
        final boolean copyItself = sa.hasParam("CopyItself");
        if (cardToCopy == null && !copyItself) {
            return;
        }

        final boolean optional = sa.hasParam("Optional");
        if (optional && !host.getController().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantCopy", cardToCopy.getTranslatedName()), null)) {
            return;
        }

        // find target of cloning i.e. card becoming a clone
        if (sa.hasParam("CloneTarget")) {
            cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
            if (cloneTargets.isEmpty()) {
                return;
            }
        } else if (sa.hasParam("Choices") && sa.usesTargeting()) {
            cloneTargets.add(sa.getTargetCard());
        } else {
            cloneTargets.add(host);
        }

        if (cloneTargets.contains(cardToCopy) && sa.hasParam("ExcludeChosen")) {
            cloneTargets.remove(cardToCopy);
        }

        // Kallist Rhoka (Swap$): the card copied also becomes a copy of the clone, as the clone was before
        final Card swapFrom = sa.hasParam("Swap") && cardToCopy != null && cloneTargets.size() == 1
                ? CardCopyService.getLKICopy(cloneTargets.get(0)) : null;

        final long ts = game.getNextTimestamp();
        // FollowsCard$: the card stays the clone in every zone it moves to until the duration ends (Standard Procedure)
        final boolean followsCard = sa.hasParam("FollowsCard");

        for (Card tgtCard : cloneTargets) {
            if (sa.hasParam("CloneZone") &&
                    !tgtCard.isInZone(ZoneType.smartValueOf(sa.getParam("CloneZone")))) {
                continue;
            }

            if (tgtCard.isPhasedOut()) {
                continue;
            }

            game.getTriggerHandler().clearActiveTriggers(tgtCard, null);

            tgtCard.addCloneState(CardFactory.getCloneStates(copyItself ? tgtCard : cardToCopy, tgtCard, sa), ts);
            if (swapFrom != null) {
                game.getTriggerHandler().clearActiveTriggers(cardToCopy, null);
                cardToCopy.addCloneState(CardFactory.getCloneStates(swapFrom, cardToCopy, sa), ts);
                cardToCopy.updateStateForView();
            }
            if (followsCard) {
                tgtCard.addPerpetual(new forge.game.card.perpetual.PerpetualClone(ts, copyItself ? tgtCard : cardToCopy, sa));
            }
            tgtCard.updateRooms();

            // set ETB tapped of clone
            if (sa.hasParam("IntoPlayTapped")) {
                tgtCard.setTapped(true);
            }

            if (!pumpKeywords.isEmpty()) {
                tgtCard.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, ts, null);
                TokenEffectBase.addPumpUntil(sa, tgtCard, ts);
            }

            tgtCard.updateStateForView();

            if (sa.hasParam("ReplaceSpell") && tgtCard.isInZone(ZoneType.Stack)) {
                replaceSpell(game, tgtCard);
            }

            // when clone is itself, cleanup from old abilities
            if (host.equals(tgtCard) && !sa.hasParam("ImprintRememberedNoCleanup")) {
                tgtCard.clearImprintedCards();
                tgtCard.clearRemembered();
            }

            if (sa.hasParam("Duration")) {
                final Card clonedObject = tgtCard;
                // if clone is temporary, target needs old values back after (keep Death-Mask Duplicant working)
                final Iterable<Card> clonedImprinted = new CardCollection(tgtCard.getImprintedCards());
                final Iterable<Object> clonedRemembered = new FCollection<>(tgtCard.getRemembered());

                final GameCommand unclone = new GameCommand() {
                    private static final long serialVersionUID = -78375985476256279L;

                    @Override
                    public void run() {
                        // a clone that follows the card ends on whatever object the card is now
                        final Card cloneCard = followsCard ? game.getCardState(clonedObject) : clonedObject;
                        if (followsCard) {
                            cloneCard.removePerpetual(ts);
                        }
                        if (copyItself && cloneCard.removeCloneState(ts)) {
                            cloneCard.updateStateForView();
                            game.fireEvent(new GameEventCardStatsChanged(cloneCard));
                        } else if (!copyItself && cloneCard.removeCloneState(ts)) {
                            // remove values gained while being cloned
                            cloneCard.clearImprintedCards();
                            cloneCard.clearRemembered();
                            // restore original Remembered and Imprinted, ignore cards from players who lost
                            cloneCard.addImprintedCards(IterableUtil.filter(clonedImprinted, CardPredicates.ownerLives()));
                            cloneCard.addRemembered(IterableUtil.filter(clonedRemembered, Player.class));
                            cloneCard.addRemembered(IterableUtil.filter(IterableUtil.filter(clonedRemembered, Card.class), CardPredicates.ownerLives()));
                            cloneCard.updateStateForView();
                            game.fireEvent(new GameEventCardStatsChanged(cloneCard));
                        }
                    }
                };

                addUntilCommand(sa, unclone);
            }

            if (!copyItself) {
                // now we can also cleanup in case target was another card
                tgtCard.clearRemembered();
                tgtCard.clearImprintedCards();

                if (sa.hasParam("RememberCloneOrigin")) {
                    tgtCard.addRemembered(cardToCopy);
                }
                // spire
                tgtCard.setMarkedColors(cardToCopy.getMarkedColors());
                // a word blank's value is copied (legacy's ruling)
                tgtCard.setLegacyWord(cardToCopy.getLegacyWord());
            }

            game.fireEvent(new GameEventCardStatsChanged(tgtCard));
        }
    }

    /**
     * Transcantation: a spell that becomes a copy of another card has that card's spell ability and no targets (its
     * rulings), and nothing from how it was cast - buyback, flashback, kicker - since those belonged to the old ability.
     * It's still the same object, so whatever targeted it still does. Its controller may then choose targets.
     */
    private static void replaceSpell(final Game game, final Card spell) {
        SpellAbilityStackInstance si = null;
        for (final SpellAbilityStackInstance s : game.getStack()) {
            if (s.isSpell() && s.getSourceCard() == spell) {
                si = s;
                break;
            }
        }
        final SpellAbility newSa = spell.getFirstSpellAbility();
        if (si == null || newSa == null || !newSa.isSpell()) {
            return;
        }
        final SpellAbility oldSa = si.getSpellAbility();
        final Player controller = oldSa.getActivatingPlayer();
        newSa.setActivatingPlayer(controller);
        newSa.setCopied(oldSa.isCopied());
        for (SpellAbility part = newSa; part != null; part = part.getSubAbility()) {
            part.resetTargets();
        }
        final SpellAbilityStackInstance replacement = game.getStack().replaceInPlace(si, newSa);
        if (replacement == null) {
            return;
        }

        for (final SpellAbilityStackInstance other : game.getStack()) {
            final TargetChoices oldTC = other.getTargetChoices().clone();
            boolean changed = false;
            for (SpellAbility part = other.getSpellAbility(); part != null; part = part.getSubAbility()) {
                if (part.usesTargeting() && part.getTargets().contains(oldSa)) {
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

        final Set<GameObject> chosen = Sets.newHashSet();
        for (SpellAbility part = newSa; part != null; part = part.getSubAbility()) {
            if (part.usesTargeting() && !controller.getController().chooseTargetsFor(part)) {
                part.resetTargets();
            }
        }
        replacement.updateTarget(new TargetChoices(), chosen);
        for (final GameObject tgt : chosen) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.SourceSA, newSa);
            runParams.put(AbilityKey.Target, tgt);
            if (tgt instanceof Card c) {
                if (!c.hasBecomeTargetThisTurn()) {
                    runParams.put(AbilityKey.FirstTime, null);
                }
                if (c.isValiant(controller)) {
                    runParams.put(AbilityKey.Valiant, null);
                }
                c.addTargetFromThisTurn(controller);
            }
            game.getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
        }
        if (!chosen.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.SourceSA, newSa);
            runParams.put(AbilityKey.Targets, chosen);
            game.getTriggerHandler().runTrigger(TriggerType.BecomesTargetOnce, runParams, false);
        }
        game.getAction().checkStaticAbilities();
        game.getTriggerHandler().resetActiveTriggers();
    }
}
