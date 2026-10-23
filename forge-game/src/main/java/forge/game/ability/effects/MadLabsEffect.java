package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterKeywordType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

/**
 * Mad Labs: starting with the next player in turn order, one player chooses a permanent type, the next a number from 1
 * to 10, the next a keyword counter. Then each player creates a token copy of a permanent of that type (they choose
 * which), puts that many +1/+1 counters on it and makes it a 0/0 creature if it isn't one; it enters with the keyword
 * counter. The AI's choices of the words are random (my call: they're blind, the second ability isn't revealed yet).
 */
public class MadLabsEffect extends SpellAbilityEffect {

    private static final List<String> PERMANENT_TYPES = List.of("Artifact", "Battle", "Creature", "Enchantment", "Land", "Planeswalker");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Mad Labs - choose a permanent type, a number and a keyword counter.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player activator = sa.getActivatingPlayer();
        final List<Player> order = Lists.newArrayList(game.getPlayersInTurnOrder(activator));
        // "starting with the next player in turn order"
        order.add(order.remove(0));

        final Player typeChooser = order.get(0);
        final Player numberChooser = order.get(1 % order.size());
        final Player counterChooser = order.get(2 % order.size());

        final String type = typeChooser.getController().isAI() ? Aggregates.random(PERMANENT_TYPES)
                : typeChooser.getController().chooseSomeType("permanent type", sa, PERMANENT_TYPES);
        final int number = numberChooser.getController().isAI() ? 1 + forge.util.MyRandom.getRandom().nextInt(10)
                : numberChooser.getController().chooseNumber(sa, "Choose a number between 1 and 10", 1, 10);
        final List<String> counters = CounterKeywordType.keywordCounterNames();
        final String keyword = counterChooser.getController().isAI() ? Aggregates.random(counters)
                : counterChooser.getController().chooseSomeType("keyword counter", sa, counters);
        if (type == null || keyword == null) {
            return;
        }
        game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Mad Labs: " + typeChooser + " chose " + type + ", "
                + numberChooser + " chose " + number + ", " + counterChooser + " chose " + keyword + ".");
        game.getAction().notifyOfValue(sa, activator, "Each player creates a token that's a copy of a " + type.toLowerCase()
                + " and puts " + number + " +1/+1 counters on it. If it's not a creature, it becomes a 0/0 creature in"
                + " addition to its other types. It enters with a " + keyword.toLowerCase() + " counter.", null);

        final CounterType keywordCounter = CounterType.getType(keyword);
        for (final Player p : game.getPlayersInTurnOrder(activator)) {
            final CardCollection choices = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), type);
            if (choices.isEmpty()) {
                return;
            }
            final Card pick = p.getController().chooseSingleEntityForEffect(choices, sa, "Choose a " + type.toLowerCase() + " to copy", false, null);
            if (pick == null) {
                continue;
            }
            host.clearRemembered();
            final SpellAbility copy = AbilityFactory.getAbility("DB$ CopyPermanent | Defined$ CardUID_" + pick.getId()
                    + " | WithCountersType$ " + keywordCounter.toString() + " | RememberTokens$ True", host);
            copy.setActivatingPlayer(p);
            AbilityUtils.resolve(copy);
            for (final Object o : Lists.newArrayList(host.getRemembered())) {
                if (!(o instanceof Card token)) {
                    continue;
                }
                final Card made = game.getCardState(token);
                final GameEntityCounterTable table = new GameEntityCounterTable();
                made.addCounter(CounterEnumType.P1P1, number, p, table);
                table.replaceCounterEffect(game, sa);
                if (!made.isCreature()) {
                    final SpellAbility animate = AbilityFactory.getAbility("DB$ Animate | Defined$ CardUID_" + made.getId()
                            + " | Types$ Creature | Power$ 0 | Toughness$ 0 | Duration$ Permanent", host);
                    animate.setActivatingPlayer(p);
                    AbilityUtils.resolve(animate);
                }
            }
            host.clearRemembered();
        }
    }
}
