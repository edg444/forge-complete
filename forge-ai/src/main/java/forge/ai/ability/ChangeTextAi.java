package forge.ai.ability;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.card.CardType;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * Changing a basic land type word in a permanent's text (Magical Hack; Mind Bend and Crystal Spray through
 * ChooseGenericAi's ChangeTextKill). What the AI uses it for: killing a creature that is sacrificed when its controller
 * controls no lands of a type its text names (Dandan, Sea Serpent), by swapping that type for one they have none of.
 */
public class ChangeTextAi extends SpellAbilityAi {

    // host card id -> {the type its text names, the type to change it to}, for the choices made as it resolves
    private static final Map<Integer, String[]> PLANS = new ConcurrentHashMap<>();

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting() && changesBasicLandType(sa) && targetKill(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        // Mind Bend's and Crystal Spray's choices change their parent's target, which ChangeTextKill picked
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean changesBasicLandType(final SpellAbility sa) {
        return sa.hasParam("ChangeTypeWord") && sa.getParam("ChangeTypeWord").startsWith("ChooseBasicLandType");
    }

    /** Targets the opposing creature whose swap kills the most, and plans the swap; false when nothing dies to one. */
    public static boolean targetKill(final Player ai, final SpellAbility sa) {
        Card best = null;
        String[] bestPlan = null;
        for (final Card c : CardLists.getTargetableCards(ai.getOpponents().getCreaturesInPlay(), sa)) {
            final String[] plan = killPlan(c);
            if (plan != null && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
                bestPlan = plan;
            }
        }
        if (best == null) {
            return false;
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        PLANS.put(sa.getHostCard().getId(), bestPlan);
        return true;
    }

    /**
     * {the basic land type in a "when you control no [type], sacrifice" state trigger, a basic land type its
     * controller controls no land of}, or null when changing the word wouldn't make it go.
     */
    static String[] killPlan(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() != TriggerType.Always || !t.hasParam("IsPresent") || !"EQ0".equals(t.getParam("PresentCompare"))) {
                continue;
            }
            final SpellAbility exec = t.ensureAbility();
            if (exec == null || exec.getApi() != ApiType.Sacrifice) {
                continue;
            }
            for (final String type : CardType.getBasicTypes()) {
                if (!t.getParam("IsPresent").startsWith(type + ".")) {
                    continue;
                }
                for (final String other : CardType.getBasicTypes()) {
                    if (!other.equals(type) && CardLists.getType(c.getController().getLandsInPlay(), other).isEmpty()) {
                        return new String[] { type, other };
                    }
                }
            }
        }
        return null;
    }

    /** As a planned swap resolves: the word to change, then the word to change it to; null when there's no plan. */
    public static String plannedType(final SpellAbility sa, final Collection<String> validTypes) {
        if (sa == null || sa.getHostCard() == null || sa.getApi() != ApiType.ChangeText) {
            return null;
        }
        final String[] plan = PLANS.get(sa.getHostCard().getId());
        if (plan == null) {
            return null;
        }
        if (validTypes.contains(plan[0])) {
            return plan[0];
        }
        if (validTypes.contains(plan[1])) {
            PLANS.remove(sa.getHostCard().getId());
            return plan[1];
        }
        return null;
    }
}
