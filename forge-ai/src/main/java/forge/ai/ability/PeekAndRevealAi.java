package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PeekAndRevealAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if ("FightAfterReveal".equals(logic)) {
            return fightAfterReveal(aiPlayer, sa);
        }
        if ("Main2".equals(logic)) {
            if (aiPlayer.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        // So far this only appears on Triggers, but will expand
        // once things get converted from Dig + NoMove
        Player opp = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
        Player libraryOwner = aiPlayer;

        if (sa.usesTargeting()) {
            sa.resetTargets();
            //todo: evaluate valid targets
            if (!sa.canTarget(opp)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.getTargets().add(opp);
            libraryOwner = opp;
        }

        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // if the AI may already look at its own top card (e.g. Iron Lad, Diverging Destiny),
        // don't activate when a sub-ability conditioned on the revealed card can't pay off
        if (libraryOwner == aiPlayer && !sa.isTrigger() && sa.hasParam("RememberRevealed")
                && "1".equals(sa.getParamOrDefault("PeekAmount", "1"))) {
            final Card topCard = aiPlayer.getCardsIn(ZoneType.Library).getFirst();
            if (topCard.mayPlayerLook(aiPlayer)) {
                for (AbilitySub sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
                    if ("Remembered".equals(sub.getParam("ConditionDefined")) && sub.hasParam("ConditionPresent")
                            && !topCard.isValid(sub.getParam("ConditionPresent").split(","), aiPlayer, sa.getHostCard(), sa)) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            }
        }

        if (libraryOwner == aiPlayer && !sa.isTrigger() && sa.isActivatedAbility() && sa.getPayCosts().hasManaCost()) {
            final PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
            final Card host = sa.getHostCard();
            final AbilitySub sub = sa.getSubAbility();
            if (sub == null && !sa.hasParam("RememberRevealed")) {
                // Brutal Deceiver: just a look at its own top card, worth once a turn at the end of the opponent's turn
                if (sa.getActivationsThisTurn() > 0 || !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(aiPlayer))
                        || aiPlayer.getCardsIn(ZoneType.Library).getFirst().mayPlayerLook(aiPlayer)) {
                    return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
                }
            } else if (sub != null && (sub.getApi() == ApiType.Pump || sub.getApi() == ApiType.Animate || sub.getApi() == ApiType.Untap)
                    && "Self".equals(sub.getParam("Defined"))) {
                // its reveal-for-a-bonus only matters once blocks are known, with the creature in the fight
                final Combat combat = aiPlayer.getGame().getCombat();
                if (combat == null || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        || !(combat.isAttacking(host) || combat.isBlocking(host))) {
                    return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
                }
            }
        }

        if ("X".equals(sa.getParam("PeekAmount")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.setMaxXValue(sa, aiPlayer, sa.isTrigger());
            if (xPay == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * Alexander Clamilton: it gets +X/+0 for the revealed card's lines of rules text, then fights the target. Without
     * a look at the top card the AI counts on 2 lines, and only picks a fight it wins and survives on that count.
     */
    private static AiAbilityDecision fightAfterReveal(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ai.getCardsIn(ZoneType.Library).isEmpty()
                || !(ph.is(PhaseType.MAIN2, ai) || (ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == ai))) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        int bonus = 2;
        final Card top = ai.getCardsIn(ZoneType.Library).getFirst();
        if (top.mayPlayerLook(ai) && top.getPrintedRulesLines() >= 0) {
            bonus = top.getPrintedRulesLines();
        }
        final int power = host.getNetPower() + bonus;
        final int toughnessLeft = host.getNetToughness() - host.getDamage();
        final CardCollection prey = CardLists.filter(CardLists.getTargetableCards(ai.getOpponents().getCreaturesInPlay(), sa),
                c -> c.getNetToughness() - c.getDamage() <= power && c.getNetPower() < toughnessLeft
                        && !ComputerUtilCombat.combatantCantBeDestroyed(c.getController(), c));
        if (prey.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(prey));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if ("InstantOrSorcery".equals(sa.getParam("AILogic"))) {
            CardCollection revealed = (CardCollection) params.get("Revealed");
            for (Card c : revealed) {
                if (!c.isInstant() && !c.isSorcery()) {
                    return false;
                }
            }
        }

        AbilitySub subAb = sa.getSubAbility();
        return subAb != null && SpellApiToAi.Converter.get(subAb).chkDrawbackWithSubs(player, subAb).willingToPlay();
    }

}
