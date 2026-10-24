package forge.ai.ability;


import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;

import java.util.Map;

public class RepeatAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        String logic = sa.getParamOrDefault("AILogic", "");

        if (sa.usesTargeting()) {
            if (!sa.canTarget(opp)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.resetTargets();
            sa.getTargets().add(opp);
        }
        if ("AdNauseam".equals(logic)) {
            return AdNauseam.timing(ai) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.WaitForEndOfTurn);
        }
        if ("MaxX".equals(logic) || "MaxXAtOppEOT".equals(logic)) {
            if ("MaxXAtOppEOT".equals(logic) && !(ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN) && ai.getGame().getPhaseHandler().getNextTurn() == ai)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            final int max = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
            if (max <= 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
    
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if ("AdNauseam".equals(sa.getParam("AILogic"))) {
            return AdNauseam.keepGoing(player);
        }
      //TODO add logic to have computer make better choice (ArsenalNut)
        return false;
    }

    // Ad Nauseam: keep revealing while even the costliest card left in the library can't put the AI in danger
    static final class AdNauseam {
        static boolean keepGoing(final Player ai) {
            if (!ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) {
                return ai.getCardsIn(ZoneType.Library).size() > 1;
            }
            if (ai.getCardsIn(ZoneType.Library).size() <= 1 || ai.getCardsIn(ZoneType.Hand).size() >= 10) {
                return false;
            }
            int worst = 0;
            for (final Card c : ai.getCardsIn(ZoneType.Library)) {
                worst = Math.max(worst, c.getCMC());
            }
            return ai.getLife() - worst > Math.max(AiProfileUtil.getIntProperty(ai, AiProps.AI_IN_DANGER_THRESHOLD), 5);
        }

        static boolean timing(final Player ai) {
            // an instant: at the end of the opponent's turn the cards are all ready for the AI's own turn
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai) && keepGoing(ai);
        }
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        String logic = sa.getParamOrDefault("AILogic", "");

        if (sa.usesTargeting()) {
            if (logic.startsWith("CopyBestCreature")) {
                Card best = null;
                Iterable<Card> targetableAi = IterableUtil.filter(ai.getCreaturesInPlay(), CardPredicates.isTargetableBy(sa));
                if (!logic.endsWith("IgnoreLegendary")) {
                    best = ComputerUtilCard.getBestAI(IterableUtil.filter(targetableAi, Card::ignoreLegendRule));
                } else {
                    best = ComputerUtilCard.getBestAI(targetableAi);
                }
                if (best == null && mandatory && sa.canTarget(sa.getHostCard())) {
                    best = sa.getHostCard();
                }
                if (best != null) {
                    sa.resetTargets();
                    sa.getTargets().add(best);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
            if (opp != null) {
                sa.resetTargets();
                sa.getTargets().add(opp);
            } else if (!mandatory) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

        }

    	// setup subability to repeat
        final SpellAbility repeat = sa.getAdditionalAbility("RepeatSubAbility");

        if (repeat == null) {
            if (mandatory) {
                return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        if (aic.doTrigger(repeat, mandatory)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }
}
