package forge.ai;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.MagicStack;

/**
 * Keywords a creature gives itself with its own triggers, such as Brightfield Glider's "whenever this creature attacks
 * while saddled, it gets +1/+2 and gains flying". The combat predictions add such a trigger's P/T but judge blocks by
 * the keywords the card has now, and a second grant of the keyword (Eddytrail Hawk) can wait beside the trigger.
 * Fork-only.
 */
public final class SelfGrantedKeywords {
    private SelfGrantedKeywords() {
    }

    /** The attacker as blockers will see it once its own attack triggers resolved, or the attacker itself. */
    public static Card asAttacking(final Card attacker, final Combat combat) {
        if (combat == null) {
            return attacker;
        }
        final List<String> gained = Lists.newArrayList();
        SpellAbility pump = null;
        for (final Trigger t : attacker.getTriggers()) {
            if (t.getMode() != TriggerType.Attacks) {
                continue;
            }
            final SpellAbility sa = selfKeywordPump(t);
            if (sa == null || !ComputerUtilCombat.combatTriggerWillTrigger(attacker, null, t, combat)) {
                continue;
            }
            for (final String kw : keywords(sa)) {
                if (!attacker.hasKeyword(kw)) {
                    gained.add(kw);
                    pump = sa;
                }
            }
        }
        if (gained.isEmpty()) {
            return attacker;
        }
        return ComputerUtilCard.getPumpedCreature(attacker.getController(), pump, attacker, 0, 0, gained);
    }

    /**
     * Whether the creature has the keyword or gets it from an ability still waiting to resolve. Simultaneous triggers
     * go on the stack one at a time, so the creature's own trigger may not be on it yet; the stack still knows it by
     * its trigger id.
     */
    public static boolean hasOrWillGain(final Card c, final String keyword, final SpellAbility except) {
        if (CardUtil.isStackingKeyword(keyword)) {
            return false;
        }
        if (c.hasKeyword(keyword)) {
            return true;
        }
        final MagicStack stack = c.getGame().getStack();
        for (final Trigger t : c.getTriggers()) {
            final SpellAbility sa = selfKeywordPump(t);
            if (sa != null && sa != except && keywords(sa).contains(keyword) && stack.hasStateTrigger(t.getId())) {
                return true;
            }
        }
        for (final SpellAbilityStackInstance si : stack) {
            SpellAbility s = si.getSpellAbility();
            if (s.isWrapper()) {
                s = ((WrappedAbility) s).getWrappedAbility();
            }
            for (; s != null; s = s.getSubAbility()) {
                if (s == except || s.getApi() != ApiType.Pump || !s.hasParam("KW") || !keywords(s).contains(keyword)) {
                    continue;
                }
                final boolean affects = s.usesTargeting() ? s.getTargets().getTargetCards().contains(c)
                        : AbilityUtils.getDefinedCards(s.getHostCard(), s.getParamOrDefault("Defined", "Self"), s).contains(c);
                if (affects) {
                    return true;
                }
            }
        }
        return false;
    }

    // only free grants count: a "you may pay" trigger is a later decision the AI may turn down
    private static SpellAbility selfKeywordPump(final Trigger t) {
        final SpellAbility sa = t.ensureAbility();
        if (sa == null || sa.getApi() != ApiType.Pump || !sa.hasParam("KW") || sa.usesTargeting() || sa.hasParam("Cost")) {
            return null;
        }
        return "Self".equals(sa.getParamOrDefault("Defined", "Self")) ? sa : null;
    }

    private static List<String> keywords(final SpellAbility sa) {
        return Arrays.asList(sa.getParam("KW").split(" & "));
    }
}
