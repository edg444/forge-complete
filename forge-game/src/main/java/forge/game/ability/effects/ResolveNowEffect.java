package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * Nah Yeah: "Target spell resolves. (It just happens right now.)" The target resolves as soon as this is done resolving,
 * before anyone gets priority (MagicStack.resolveRightAfter).
 */
public class ResolveNowEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Target spell resolves.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final SpellAbility target : getTargetSpells(sa)) {
            final SpellAbilityStackInstance si = sa.getHostCard().getGame().getStack().getInstanceMatchingSpellAbilityID(target);
            if (si != null) {
                sa.getHostCard().getGame().getStack().resolveRightAfter(si);
                return;
            }
        }
    }
}
