package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;

/**
 * Backsies: until end of turn, every counter on every permanent and player is treated as a CounterType$ counter (see
 * GameEntity.getCounters). A +1/+1 counter then shrinks its creature, and loyalty counters stop counting as loyalty.
 */
public class TreatCountersAsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Until end of turn, treat all counters as " + CounterType.getType(sa.getParam("CounterType")).getName() + " counters.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        game.setCountersTreatedAs(CounterType.getType(sa.getParam("CounterType")));
        game.getEndOfTurn().addUntil(new GameCommand() {
            private static final long serialVersionUID = 1L;

            @Override
            public void run() {
                game.setCountersTreatedAs(null);
            }
        });
        game.getAction().checkStateEffects(true);
    }
}
