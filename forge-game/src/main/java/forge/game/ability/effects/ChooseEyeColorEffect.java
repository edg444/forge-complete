package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.MagicColor;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

// Avatar of Me. Eyes can be any colour at all, not just the five, so this is one plain list rather
// than a colour picker that has to somehow offer "none" - the generic multi-select won't accept an
// empty choice. A colour that isn't one of the five leaves nothing chosen, which makes the card
// colourless, and the name is kept only so it can be displayed.
public class ChooseEyeColorEffect extends SpellAbilityEffect {

    private static final List<String> OTHER = Lists.newArrayList(
            "Brown", "Hazel", "Amber", "Gray", "Violet", "Another color");

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " is the color of your eyes.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final List<String> choices = Lists.newArrayList();
            for (final String color : MagicColor.Constant.ONLY_COLORS) {
                choices.add(capitalize(color));
            }
            choices.addAll(OTHER);
            choices.add("Two different colors");

            final String chosen = p.getController().chooseSomeType("Eye color", sa, choices);
            if (chosen == null) {
                continue;
            }

            if ("Two different colors".equals(chosen)) {
                // heterochromia makes it both, so exactly two are picked and both are real colours
                final List<String> two = Lists.newArrayList();
                for (final String color : MagicColor.Constant.ONLY_COLORS) {
                    two.add(capitalize(color));
                }
                final String first = p.getController().chooseSomeType("First eye color", sa, two);
                two.remove(first);
                final String second = p.getController().chooseSomeType("Second eye color", sa, two);
                host.setChosenColors(Lists.newArrayList(first.toLowerCase(), second.toLowerCase()));
                host.setChosenType("");
            } else if (OTHER.contains(chosen)) {
                host.setChosenColors(Lists.newArrayList());
                host.setChosenType(chosen);
            } else {
                host.setChosenColors(Lists.newArrayList(chosen.toLowerCase()));
                host.setChosenType("");
            }
            host.getGame().getAction().notifyOfValue(sa, host, chosen, p);
        }
    }

    private static String capitalize(final String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
