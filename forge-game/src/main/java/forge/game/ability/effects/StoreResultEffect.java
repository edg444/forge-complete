package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Goblin Girder Gang: storing a die result (Amount$) on the host, unless that result is stored there already - checked as
 * it resolves too (rulings), so a result rolled twice is stored once. Any result counts, 0 and above 20 included. The
 * host's Stored SVar says whether it stored one, for a "when you do" trigger.
 */
public class StoreResultEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Store the result on " + sa.getHostCard() + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int result = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);
        final List<Integer> stored = host.getStoredRolls();
        if (!host.isInPlay() || (stored != null && stored.contains(result))) {
            host.setSVar("Stored", "Number$0");
            return;
        }
        host.addStoredRolls(List.of(result));
        host.setSVar("Stored", "Number$1");
    }
}
