package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.FlumeLog;
import forge.game.spellability.SpellAbility;

/** Log Flume: the defined creatures jump in a log until end of turn (FlumeLog). */
public class JumpInLogEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return getDefinedCardsOrTargeted(sa) + " jump in a log until end of turn.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final CardCollection creatures = new CardCollection();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            if (c.isInPlay() && c.isCreature()) {
                creatures.add(c);
            }
        }
        if (creatures.isEmpty()) {
            return;
        }
        final FlumeLog log = new FlumeLog(sa.getActivatingPlayer(), creatures);
        for (final Card c : creatures) {
            c.setFlumeLog(log);
        }
        addUntilCommand(sa, () -> {
            for (final Card c : creatures) {
                if (c.getFlumeLog() == log) {
                    c.setFlumeLog(null);
                }
            }
        });
    }
}
