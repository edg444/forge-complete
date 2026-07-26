package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

// Asks the person playing about themselves (Avatar of Me's height and shoe size) and keeps the
// answer for the game. It has to be asked here rather than where the number is used, because the
// static ability that reads it recalculates during state checks where prompting isn't possible.
public class AskFactEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getParamOrDefault("Title", "Answer a question about yourself.");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final String fact = sa.getParam("Fact");
        final String title = sa.getParamOrDefault("Title", "Choose a number");
        final int min = Integer.parseInt(sa.getParamOrDefault("Min", "0"));
        final int max = Integer.parseInt(sa.getParamOrDefault("Max", "100"));

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (p.hasPersonalFact(fact) && !sa.hasParam("Again")) {
                continue;
            }
            p.setPersonalFact(fact, p.getController().chooseNumber(sa, title, min, max));
        }
    }
}
