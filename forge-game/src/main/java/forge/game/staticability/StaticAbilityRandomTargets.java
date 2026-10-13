package forge.game.staticability;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

/**
 * High Troller: "All targeted spells and abilities ... have their targets chosen randomly." Its rulings: the controller
 * still chooses how many targets ("any number", "up to"), then they're chosen at random from among legal targets, in
 * the order the card lists them, never picking one that would leave a later target with no legal choice.
 */
public class StaticAbilityRandomTargets {

    public static boolean applies(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.RandomTargets) && stAb.matchesValidParam("ValidSA", sa)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Re-picks every target of the spell or ability and its sub-abilities at random, keeping how many there are. */
    public static void randomizeTargets(final SpellAbility sa) {
        for (SpellAbility part = sa; part != null; part = part.getSubAbility()) {
            if (!part.usesTargeting() || part.getTargets().isEmpty()) {
                continue;
            }
            final TargetChoices original = part.getTargets();
            final List<GameObject> chosen = Lists.newArrayList(original);
            final List<Integer> divided = Lists.newArrayList();
            for (final GameObject o : chosen) {
                divided.add(part.isDividedAsYouChoose() ? original.getDividedValue(o) : null);
            }

            final TargetChoices picked = new TargetChoices();
            part.setTargets(picked);
            boolean complete = true;
            for (int i = 0; i < chosen.size(); i++) {
                final List<GameObject> candidates = candidates(part);
                // a pick that leaves a later target with nothing legal isn't made (ruling 5)
                final int left = chosen.size() - i - 1;
                GameObject pick = null;
                while (!candidates.isEmpty()) {
                    final GameObject c = Aggregates.random(candidates);
                    candidates.remove(c);
                    picked.add(c);
                    if (left == 0 || candidates(part).size() >= left) {
                        pick = c;
                        break;
                    }
                    picked.remove(c);
                }
                if (pick == null) {
                    complete = false;
                    break;
                }
                if (divided.get(i) != null) {
                    picked.addDividedAllocation(pick, divided.get(i));
                }
            }
            if (!complete) {
                part.setTargets(original); // nothing legal to randomize to: keep what was chosen
            }
        }
    }

    private static List<GameObject> candidates(final SpellAbility part) {
        final List<GameObject> result = Lists.newArrayList();
        for (final GameEntity e : part.getTargetRestrictions().getAllCandidates(part, false)) {
            if (!part.getTargets().contains(e)) {
                result.add(e);
            }
        }
        if (part.getTargetRestrictions().getZone().contains(ZoneType.Stack)) {
            for (final SpellAbilityStackInstance si : part.getHostCard().getGame().getStack()) {
                final SpellAbility onStack = si.getSpellAbility();
                if (onStack != null && !part.getTargets().contains(onStack) && part.canTargetSpellAbility(onStack)) {
                    result.add(onStack);
                }
            }
        }
        return result;
    }
}
