package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.cost.Cost;
import forge.game.judgetower.Judge;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.GameActionUtil;

/**
 * Judge Tower for an AI seat: it doesn't weigh up what's worth doing, since everything that can be done must be. It
 * only chooses how, and a target it would rather not pick is still picked.
 */
final class JudgeTowerAi {
    private JudgeTowerAi() { }

    /** The next thing the player must do, ready to play; null to pass. */
    static List<SpellAbility> chooseAction(final Player player, final AiController ai) {
        final Judge judge = player.getGame().getJudge();
        for (final SpellAbility action : judge.mandatoryActions(player)) {
            if (ai.hasFailedToCast(action)) {
                continue;
            }
            final SpellAbility ready = prepare(action, player, judge, ai);
            if (ready != null) {
                return Collections.singletonList(ready);
            }
            ai.noteFailedCast(AiController.castKey(action));
        }
        return null;
    }

    /** As many modes as it can have: an optional one must be used if able, so it's not a question of which are good. */
    static List<AbilitySub> chooseModes(final List<AbilitySub> possible, final int num) {
        final List<AbilitySub> modes = new ArrayList<>(possible);
        return modes.subList(0, Math.min(num, modes.size()));
    }

    private static SpellAbility prepare(final SpellAbility action, final Player player, final Judge judge, final AiController ai) {
        SpellAbility sa = action;
        sa.setActivatingPlayer(player);
        if (sa.isLandAbility()) {
            return sa;
        }
        final List<OptionalCostValue> optional = GameActionUtil.getOptionalCostValues(sa);
        if (!optional.isEmpty()) {
            sa = GameActionUtil.addOptionalCosts(sa, judge.requiredOptionalCosts(sa, optional));
            sa.setActivatingPlayer(player);
        }
        final Cost cost = sa.getPayCosts();
        final boolean usesX = (cost != null && cost.hasXInAnyCostPart()) || sa.getParamOrDefault("Announce", "").contains("X");
        if (usesX) {
            sa.setXManaCostPaid(judge.requiredX(sa, player, Integer.MAX_VALUE));
        }
        if (sa.getApi() != ApiType.Charm && !chooseTargets(sa, ai, player)) {
            return null;
        }
        // an AI's targeting logic works out what it would like X to be
        if (usesX) {
            sa.setXManaCostPaid(judge.requiredX(sa, player, Integer.MAX_VALUE));
        }
        return ComputerUtilCost.canPayCost(sa, player, false) ? sa : null;
    }

    /** The AI's own pick if it has one when it has no choice, or else any legal target. False if there's none to be had. */
    private static boolean chooseTargets(final SpellAbility sa, final AiController ai, final Player player) {
        boolean uses = false;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            uses |= a.usesTargeting();
        }
        if (!uses) {
            return true;
        }
        sa.resetTargets();
        for (AbilitySub sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            sub.resetTargets();
        }
        try {
            ai.doTrigger(sa, true);
        } catch (final RuntimeException ex) {
            // some AI logic isn't written for "you have to": the fallback below still gets it a legal target
            System.err.println("Judge Tower: AI targeting of " + sa + " failed: " + ex);
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.usesTargeting()) {
                fillTargets(a);
                if (!a.isTargetNumberValid()) {
                    return false;
                }
            }
        }
        return player.getGame().getStack().hasLegalTargeting(sa);
    }

    private static void fillTargets(final SpellAbility sa) {
        if (sa.isTargetNumberValid()) {
            return;
        }
        final List<GameEntity> candidates = sa.getTargetRestrictions().getAllCandidates(sa);
        Collections.shuffle(candidates, forge.util.MyRandom.getRandom());
        for (final GameEntity e : candidates) {
            if (sa.isTargetNumberValid()) {
                return;
            }
            if (!sa.getTargets().contains((GameObject) e)) {
                sa.getTargets().add(e);
            }
        }
    }
}
