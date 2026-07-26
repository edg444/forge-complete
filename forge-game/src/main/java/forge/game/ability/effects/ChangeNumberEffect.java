package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

// Look at Me, I'm R&D. The chooser picks a number and then one either side of it, and from then on
// every printed instance of the first is read as the second - rules text, mana cost, power,
// toughness and collector number alike (see Game.changeNumber for where each is applied).
// Clear$ True ends it again when the enchantment leaves.
public class ChangeNumberEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard().getName() + " - choose a number to change.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        if (sa.hasParam("Clear")) {
            game.setNumberChange(null, 0);
            return;
        }

        final List<Player> choosers = getDefinedPlayersOrTargeted(sa);
        if (choosers.isEmpty()) {
            return;
        }
        final Player chooser = choosers.get(0);

        final int min = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Min", "0"), sa);
        final int max = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Max", "20"), sa);
        final int from = chooser.getController().chooseNumber(sa, "Choose a number to change", min, max);

        // the second number must be one either side, so it's offered as a choice between the two
        // rather than a free pick that could be illegal. 0 may become -1, per the rulings.
        final List<String> options = Lists.newArrayList(String.valueOf(from + 1), String.valueOf(from - 1));
        final String picked = chooser.getController().chooseSomeType("Number", sa, options);
        final int to = picked == null ? from + 1 : Integer.parseInt(picked);

        game.setNumberChange(from, to);
        game.getAction().notifyOfValue(sa, host,
                "All printed " + from + "s are now " + to + "s.", chooser);
    }
}
