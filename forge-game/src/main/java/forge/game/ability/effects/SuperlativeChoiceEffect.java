package forge.game.ability.effects;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardThreat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Superlatorium: an opponent chooses "the fastest," "the scariest," or "the weakest"; you choose a creature; then a
 * person outside the game chooses the creature that best fits. The person is simulated (user, 2026-10-21): they judge
 * by the creatures' stats - fastest by haste, evasion and power, scariest by impact, weakest by low power and toughness -
 * and usually take the best fit, sometimes the next best, now and then any creature. The host's SameChoice SVar is 1 if
 * they chose the creature you did.
 */
public class SuperlativeChoiceEffect extends SpellAbilityEffect {

    public static final List<String> CRITERIA = List.of("the fastest", "the scariest", "the weakest");

    public static ToDoubleFunction<Card> fit(final String criterion) {
        return switch (criterion) {
            case "the fastest" -> c -> (c.hasKeyword(Keyword.HASTE) ? 3 : 0) + (c.hasKeyword(Keyword.FLYING) ? 2 : 0)
                    + (c.hasKeyword(Keyword.FIRST_STRIKE) || c.hasKeyword(Keyword.DOUBLE_STRIKE) ? 1 : 0) + c.getNetPower() / 2.0;
            case "the scariest" -> CardThreat::evaluate;
            default -> c -> -(c.getNetPower() + c.getNetToughness());
        };
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "An opponent chooses a superlative; " + sa.getActivatingPlayer()
                + " and a person outside the game each choose the creature that fits it best.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("SameChoice", "Number$0");
        final CardCollection creatures = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Card::isCreature);
        if (creatures.isEmpty() || you.getOpponents().isEmpty()) {
            return;
        }
        final Player opponent = you.getOpponents().size() == 1 ? you.getOpponents().getFirst()
                : you.getController().chooseSingleEntityForEffect(you.getOpponents(), sa, "Choose an opponent to choose", null);
        final String criterion = opponent.getController().isAI() ? Aggregates.random(CRITERIA)
                : opponent.getController().chooseSomeType("superlative", sa, CRITERIA);
        game.getAction().notifyOfValue(sa, opponent, criterion, null);

        final ToDoubleFunction<Card> fit = fit(criterion);
        final List<Card> ranked = Lists.newArrayList(creatures);
        ranked.sort(Comparator.comparingDouble(fit).reversed());

        final Card yours = you.getController().isAI() ? ranked.get(0)
                : you.getController().chooseSingleEntityForEffect(creatures, sa, "Choose the creature that's " + criterion, null);
        final int roll = MyRandom.getRandom().nextInt(100);
        final Card theirs = roll < 60 || ranked.size() == 1 ? ranked.get(0) : roll < 85 ? ranked.get(1) : Aggregates.random(ranked);
        game.getGameLog().add(GameLogEntryType.INFORMATION, "A person outside the game says " + theirs + " is " + criterion + ".");
        game.getAction().notifyOfValue(sa, you, "A person outside the game chose " + theirs + " as " + criterion, null);
        if (theirs.equals(yours)) {
            host.setSVar("SameChoice", "Number$1");
        }
    }
}
