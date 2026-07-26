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

        // a number with a fraction may be chosen, so the whole list is offered in halves
        final List<String> numbers = Lists.newArrayList();
        for (int halves = min * 2; halves <= max * 2; halves++) {
            numbers.add(label(halves));
        }
        final String pickedFrom = chooser.getController().chooseSomeType("Number", sa, numbers);
        if (pickedFrom == null) {
            return;
        }
        final int from = numbers.indexOf(pickedFrom) + min * 2;

        // the second number must be one either side, so it's offered as a choice between the two
        // rather than a free pick that could be illegal. 0 may become -1, and 1/2 may become -1/2,
        // per the rulings - one higher or lower means a whole number apart either way.
        final List<String> options = Lists.newArrayList(label(from + 2), label(from - 2));
        final String picked = chooser.getController().chooseSomeType("Number", sa, options);
        final int to = picked == null ? from + 2 : (picked.equals(options.get(0)) ? from + 2 : from - 2);

        game.setNumberChange(from, to);
        game.getAction().notifyOfValue(sa, host,
                "All printed " + label(from) + "s are now " + label(to) + "s.", chooser);
    }

    /** Render a count of halves the way it's printed: 3, 2½, -½. */
    private static String label(final int halves) {
        final int whole = Math.floorDiv(halves, 2);
        if (Math.floorMod(halves, 2) == 0) {
            return String.valueOf(whole);
        }
        return whole == -1 ? "-½" : (whole == 0 ? "½" : whole + "½");
    }
}
