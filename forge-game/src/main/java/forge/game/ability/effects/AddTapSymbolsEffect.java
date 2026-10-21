package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Sole Performer: "Add {T}{T}." The {T}s go to the activator's pool, which empties as each step and phase ends, as mana
 * does, and are spent on {T} in activated abilities' costs instead of tapping the permanent (CostTap). They aren't
 * mana, so this isn't a mana ability and nothing that cares about mana sees them (rulings).
 */
public class AddTapSymbolsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final int n = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "1"), sa);
        return sa.getActivatingPlayer() + " adds " + "{T}".repeat(Math.max(0, n)) + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final int n = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "1"), sa);
        if (n <= 0) {
            return;
        }
        p.addTapSymbols(n);
        p.getGame().getGameLog().add(forge.game.GameLogEntryType.MANA, p + " adds " + "{T}".repeat(n)
                + " (" + p.getTapSymbols() + " in their pool until this step ends).");
    }
}
