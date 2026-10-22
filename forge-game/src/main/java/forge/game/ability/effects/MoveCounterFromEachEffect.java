package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Takesies: move up to one counter from each other permanent onto the target permanent. The activator picks which
 * permanents give one, then which kind of counter each gives. The AI takes one from each opponent's permanent.
 */
public class MoveCounterFromEachEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Move up to one counter from each permanent onto " + getTargetCards(sa) + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        for (final Card dest : getTargetCards(sa)) {
            final Card cur = game.getCardState(dest);
            if (!cur.isInPlay()) {
                continue;
            }
            final CardCollection withCounters = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                    c -> c != cur && c.hasCounters());
            final List<Card> sources;
            if (you.getController().isAI()) {
                sources = CardLists.filter(withCounters, c -> you.isOpponentOf(c.getController()));
            } else {
                sources = Lists.newArrayList(you.getController().chooseCardsForEffect(withCounters, sa,
                        "Choose the permanents to move a counter from", 0, withCounters.size(), true, null));
            }
            final GameEntityCounterTable table = new GameEntityCounterTable();
            for (final Card source : sources) {
                final List<CounterType> kinds = Lists.newArrayList(source.getCounters().elementSet());
                final CounterType kind = you.getController().chooseCounterType(kinds, sa,
                        "Choose the counter to move from " + source, null);
                if (kind == null || !source.canRemoveCounters(kind) || !cur.canReceiveCounters(kind)) {
                    continue;
                }
                if (source.subtractCounter(kind, 1, you) > 0) {
                    cur.addCounter(kind, 1, you, table);
                }
            }
            table.replaceCounterEffect(game, sa);
        }
    }
}
