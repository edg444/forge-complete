package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * Pirk, Heroic Captain: "Name it." The activating player gives each defined card a name of their own. A token is simply
 * created with that name, so its copies have it too; anything else has its name changed for as long as it stays put.
 */
public class NameItEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Name " + getDefinedCardsOrTargeted(sa) + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            final String name = sa.getActivatingPlayer().getController().guessString(sa, sa.getParamOrDefault("Prompt", "Name " + c + ":"));
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            if (c.isToken()) {
                c.setName(name.trim());
                c.updateNameforView();
            } else {
                c.addChangedName(name.trim(), false, c.getGame().getNextTimestamp(), 0);
            }
            c.getGame().getGameLog().add(forge.game.GameLogEntryType.STACK_RESOLVE,
                    sa.getActivatingPlayer() + " names it " + name.trim() + ".");
        }
    }
}
