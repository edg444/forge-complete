package forge.ai.ability;


import forge.ai.AiAbilityDecision;
import forge.ai.AiAttackController;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpecialAiLogic;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.Map;

public class BranchAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("GrislySigil".equals(aiLogic)) {
            boolean result = SpecialCardAi.GrislySigil.consider(aiPlayer, sa);
            return new AiAbilityDecision(result ? 100 : 0, result ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        } else if ("BranchCounter".equals(aiLogic)) {
            boolean result = SpecialAiLogic.doBranchCounterspellLogic(aiPlayer, sa);
            return new AiAbilityDecision(result ? 100 : 0, result ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        } else if ("TgtAttacker".equals(aiLogic)) {
            final Combat combat = aiPlayer.getGame().getCombat();
            if (combat == null || combat.getAttackingPlayer() != aiPlayer) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            final CardCollection attackers = combat.getAttackers();
            final CardCollection attackingBattle = CardLists.filter(attackers, card -> {
                final GameEntity def = combat.getDefenderByAttacker(combat.getBandOfAttacker(card));
                return def instanceof Card && ((Card)def).isBattle();
            });

            if (!attackingBattle.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackingBattle));
            } else {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackers));
            }

            return new AiAbilityDecision(sa.isTargetNumberValid() ? 100 : 0, sa.isTargetNumberValid() ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        }

        // TODO: expand for other cases where the AI is needed to make a decision on a branch
        if (sa.usesTargeting() && !sa.isTargetNumberValid() && !chooseTargets(aiPlayer, sa, false)) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        AiAbilityDecision decision = canPlay(aiPlayer, sa);
        if (decision.willingToPlay()) {
            return decision;
        }
        if (mandatory && (!sa.usesTargeting() || sa.isTargetNumberValid() || chooseTargets(aiPlayer, sa, true))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * Branch itself never chose targets, so every targeted Branch without its own AILogic reached the stack with
     * none and was dropped ("failed to target"): Thought-Stalker Warlock in an AI vs AI game, 2026-10-24. The
     * branches all do something to the target, so aim at an opponent or their best card; a target limited to the
     * AI's own cards (Archmage's Newt, Meticulous Excavation) is taken only from a trigger or when it must be.
     */
    private static boolean chooseTargets(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        if (tgt.getZone().contains(ZoneType.Stack)) {
            return false;
        }
        if (tgt.canTgtPlayer()) {
            Player preferred = AiAttackController.choosePreferredDefenderPlayer(ai);
            if (preferred != null && preferred.isOpponentOf(ai) && sa.canTarget(preferred)) {
                sa.getTargets().add(preferred);
                return true;
            }
            for (Player opp : ai.getOpponents()) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                    return true;
                }
            }
        }
        final CardCollection cards = CardUtil.getValidCardsToTarget(sa);
        final CardCollection theirs = CardLists.filterControlledBy(cards, ai.getOpponents());
        if (!theirs.isEmpty()) {
            sa.getTargets().add(ComputerUtilCard.getBestAI(theirs));
            return true;
        }
        if (tgt.getMinTargets(sa.getHostCard(), sa) == 0) {
            return true;
        }
        if (!mandatory && !sa.isTrigger()) {
            return false;
        }
        if (!cards.isEmpty()) {
            final CardCollection mine = CardLists.filterControlledBy(cards, ai);
            sa.getTargets().add(mine.size() == cards.size() ? ComputerUtilCard.getBestAI(mine) : ComputerUtilCard.getWorstAI(cards));
            return true;
        }
        if (mandatory && tgt.canTgtPlayer() && sa.canTarget(ai)) {
            sa.getTargets().add(ai);
            return true;
        }
        return false;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
