package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * AILogic$ StackClear (Swift Silence, Summary Dismissal, Mindbreak Trap): counter or exile every other spell on the
 * stack - when what the opponents have there is worth it and nothing of its own would go with it (a targeted one like
 * Mindbreak Trap takes only theirs).
 */
public final class StackClearAi {

    private StackClearAi() { }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        int theirs = 0;
        boolean mine = false;
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s.getHostCard() != null && s.getHostCard().equals(sa.getHostCard())) {
                continue;
            }
            if (s.getActivatingPlayer().isOpponentOf(ai)) {
                theirs += s.isSpell() ? 40 + s.getHostCard().getCMC() * 20 : 30;
            } else if (s.getActivatingPlayer().equals(ai)) {
                mine = true;
            }
        }
        if (sa.usesTargeting()) {
            sa.resetTargets();
            for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
                final SpellAbility s = si.getSpellAbility();
                if (s.getActivatingPlayer().isOpponentOf(ai) && sa.canTargetSpellAbility(s) && sa.canAddMoreTarget()) {
                    sa.getTargets().add(s);
                }
            }
            if (sa.getTargets().isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (mine) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return theirs >= 100 ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
