package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PhasesAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // This still needs to be fleshed out
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        List<Card> tgtCards;
        if (tgt == null) {
            tgtCards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if (tgtCards.contains(source)) {
                // Protect it from something
                final boolean isThreatened = ComputerUtil.predictThreatenedObjects(aiPlayer, null, true).contains(source);
                if (isThreatened) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt == null) {
            if (mandatory) {
                return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (phasesPrefTargeting(tgt, sa, mandatory)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (mandatory) {
            // not enough preferred targets, but mandatory so keep going:
            if (sa.isTargetNumberValid()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                // no valid targets, but mandatory so try to find something
                if (phasesUnpreferredTargeting(aiPlayer.getGame(), sa, mandatory)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    sa.resetTargets();
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null) {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private boolean phasesPrefTargeting(final TargetRestrictions tgt, final SpellAbility sa,
            final boolean mandatory) {
        final Player ai = sa.getActivatingPlayer();
        final Game game = ai.getGame();
        sa.resetTargets();
        final List<Card> list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
        final List<Card> theirs = CardLists.filter(list, c -> c.getController().isOpponentOf(ai));
        // Oubliette: gone until it leaves, so it's removal
        if (sa.hasParam("WontPhaseInNormal")) {
            final Card best = ComputerUtilCard.getBestCreatureAI(theirs);
            if (best != null) {
                sa.getTargets().add(best);
                return true;
            }
            return false;
        }
        // its own permanent, out of the way of removal
        if (!game.getStack().isEmpty()) {
            final List<forge.game.GameObject> doomed = ComputerUtil.predictThreatenedObjects(ai, null, true);
            final Card save = ComputerUtilCard.getBestAI(CardLists.filter(list, c -> c.getController().equals(ai) && doomed.contains(c)));
            if (save != null) {
                sa.getTargets().add(save);
                return true;
            }
            return false;
        }
        final forge.game.combat.Combat combat = game.getCombat();
        if (combat == null) {
            return false;
        }
        if (ai.equals(combat.getAttackingPlayer())) {
            // a blocker that would kill its attacker: the attacker stays blocked, but lives
            if (!game.getPhaseHandler().is(forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return false;
            }
            for (final Card att : combat.getAttackers()) {
                for (final Card b : combat.getBlockers(att)) {
                    if (theirs.contains(b) && ComputerUtilCombat.canDestroyAttacker(ai, att, b, combat, false)) {
                        sa.getTargets().add(b);
                        return true;
                    }
                }
            }
            return false;
        }
        // their biggest attacker leaves combat
        Card worst = null;
        int most = 0;
        for (final Card att : theirs) {
            if (!combat.isAttacking(att)) {
                continue;
            }
            final int dmg = combat.isUnblocked(att) ? ComputerUtilCombat.damageIfUnblocked(att, ai, combat, false) : 0;
            final boolean killsBlocker = combat.getBlockers(att).anyMatch(b -> ComputerUtilCombat.canDestroyBlocker(ai, b, att, combat, false));
            final int v = dmg + (killsBlocker ? 4 : 0);
            if (v > most) {
                most = v;
                worst = att;
            }
        }
        if (worst != null && (most >= 4 || ComputerUtilCombat.lifeInDanger(ai, combat))) {
            sa.getTargets().add(worst);
            return true;
        }
        return false;
    }

    private boolean phasesUnpreferredTargeting(final Game game, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();

        CardCollectionView list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

        // in general, if it's our own creature, choose the weakest one, if it's the opponent's creature,
        // choose the strongest one
        if (!list.isEmpty()) {
            Predicate<Card> isController = CardPredicates.isController(source.getController());
            CardCollectionView oppList = CardLists.filter(list, isController.negate());
            sa.resetTargets();
            sa.getTargets().add(!oppList.isEmpty() ? ComputerUtilCard.getBestAI(oppList) : ComputerUtilCard.getWorstAI(list));
            return true;
        }

        return false;
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // TODO: improve the selection logic, e.g. for cards like Change of Plans. Currently will
        //  confirm everything unless AILogic is "DontPhaseOut", in which case it'll confirm nothing.
        if ("DontPhaseOut".equals(sa.getParam("AILogic"))) {
            return null;
        }

        return super.chooseSingleEntity(ai, sa, options, isOptional, targetedPlayer, params);
    }
}
