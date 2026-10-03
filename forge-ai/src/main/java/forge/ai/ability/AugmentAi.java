package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCastWithFlash;
import forge.game.zone.ZoneType;

import java.util.Map;

public class AugmentAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // the combined creature is controlled by the host's controller, so only ever augment our own hosts
        CardCollection hosts = CardLists.filter(CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa),
                c -> c.getController().equals(ai));
        if (hosts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // Teacher's Pet: the sacrifice is wasted if the search can't find anything
        if (sa.hasParam("ChangeType") && CardLists.getValidCards(ai.getCardsIn(ZoneType.Library),
                sa.getParam("ChangeType"), ai, sa.getHostCard(), sa).isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // Ninja: an augment that can go on any time we could cast an instant goes on an unblocked attacker,
        // so the combined creature's combat damage trigger fires this turn
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (StaticAbilityCastWithFlash.anyWithFlash(sa, sa.getHostCard(), ai)) {
            final Combat combat = ai.getGame().getCombat();
            if (ph.isPlayerTurn(ai) && ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && combat != null) {
                final CardCollection unblocked = CardLists.filter(hosts,
                        c -> combat.isAttacking(c) && combat.isUnblocked(c));
                if (unblocked.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
                }
                hosts = unblocked;
            } else if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && hosts.anyMatch(c -> ComputerUtilCard.doesCreatureAttackAI(ai, c) || (combat != null && combat.isAttacking(c)))) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            } else if (!ph.isPlayerTurn(ai) && !ph.is(PhaseType.END_OF_TURN)) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
        }
        final Card best = ComputerUtilCard.getBestCreatureAI(hosts);
        sa.resetTargets();
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Dr. Julius Jumblemorph's "you may search": worth it whenever there's a card with augment to find
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory || !sa.hasParam("ChangeType")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        final CardCollection found = CardLists.getValidCards(
                ai.getCardsIn(ZoneType.listValueOf(sa.getParamOrDefault("SearchZones", "Library"))),
                sa.getParam("ChangeType"), ai, sa.getHostCard(), sa);
        return found.isEmpty() ? new AiAbilityDecision(0, AiPlayDecision.CantPlayAi)
                : new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Teacher's Pet's library search: always take one - the host is already chosen and the cost paid
    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return ComputerUtilCard.getBestAI(options);
    }
}
