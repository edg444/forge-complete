package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * Blufferfish: "An opponent tells you a statement about themselves and secretly notes whether it's true or false. You
 * guess which it is." A person makes up their own statement and notes it (honor system - they're the authority, and
 * lying isn't allowed). The AI states a fact about its own hidden cards, made false half the time, and notes the truth
 * from the game itself (user, 2026-10-21). An AI guessing has nothing to go on, so it guesses at random. A right guess
 * runs GuessCorrect$.
 */
public class BluffEffect extends SpellAbilityEffect {

    private static final List<String> TF = List.of("True", "False");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "An opponent tells " + sa.getActivatingPlayer() + " a statement about themselves, and "
                + sa.getActivatingPlayer() + " guesses whether it's true.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final PlayerCollection opps = you.getOpponents();
        if (opps.isEmpty()) {
            return;
        }
        final Player teller = opps.size() == 1 ? opps.get(0)
                : you.getController().chooseSingleEntityForEffect(opps, sa, "Choose an opponent to tell you a statement", null);

        final String statement;
        final boolean truth;
        if (teller.getController().isAI()) {
            final Statement s = aiStatement(teller, MyRandom.getRandom().nextBoolean());
            statement = s.text;
            truth = s.truth;
        } else {
            statement = null;
            truth = "True".equals(teller.getController().chooseStringForEffect(TF, sa,
                    "Tell " + you + " a statement about yourself, then secretly note: is it true or false?"));
        }

        final String said = statement == null ? teller + " told " + you + " a statement about themselves."
                : teller + " says: \"" + statement + "\"";
        game.getGameLog().add(GameLogEntryType.INFORMATION, said);
        final boolean guess;
        if (you.getController().isAI()) {
            guess = MyRandom.getRandom().nextBoolean();
        } else {
            guess = "True".equals(you.getController().chooseStringForEffect(TF, sa,
                    said + " Is it true or false?"));
        }

        final boolean right = guess == truth;
        final String result = you + " guessed " + (guess ? "true" : "false") + "; " + teller + "'s statement was "
                + (truth ? "true" : "false") + (right ? " - a correct guess!" : ".");
        game.getGameLog().add(GameLogEntryType.INFORMATION, result);
        for (final Player p : game.getPlayers()) {
            p.getController().notifyOfValue(sa, sa.getHostCard(), result);
        }
        if (right && sa.getAdditionalAbility("GuessCorrect") != null) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessCorrect"));
        }
    }

    public record Statement(String text, boolean truth) { }

    /** A statement about the AI's own hidden cards, true or false as asked. */
    public static Statement aiStatement(final Player ai, final boolean wantTrue) {
        final List<Statement> candidates = Lists.newArrayList();
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        final int lands = (int) hand.stream().filter(Card::isLand).count();
        final int creatures = (int) hand.stream().filter(Card::isCreature).count();
        final int spells = (int) hand.stream().filter(c -> c.isInstant() || c.isSorcery()).count();
        final int library = ai.getCardsIn(ZoneType.Library).size();

        candidates.add(new Statement("I have a land card in my hand.", lands > 0));
        candidates.add(new Statement("I have no land cards in my hand.", lands == 0));
        for (final int n : new int[] {creatures, creatures + 1, Math.max(0, creatures - 1)}) {
            candidates.add(new Statement("I have exactly " + n + " creature card" + (n == 1 ? "" : "s") + " in my hand.",
                    creatures == n));
        }
        for (final int n : new int[] {spells, spells + 1}) {
            candidates.add(new Statement("I have exactly " + n + " instant and sorcery card" + (n == 1 ? "" : "s")
                    + " in my hand.", spells == n));
        }
        final int around = Math.max(0, library - 2 + MyRandom.getRandom().nextInt(5));
        candidates.add(new Statement("My library has more than " + around + " cards in it.", library > around));
        if (library > 0) {
            final Card top = ai.getCardsIn(ZoneType.Library).get(0);
            candidates.add(new Statement("The top card of my library is a land.", top.isLand()));
            candidates.add(new Statement("The top card of my library is a creature card.", top.isCreature()));
        }
        final List<Statement> fitting = Lists.newArrayList();
        for (final Statement s : candidates) {
            if (s.truth == wantTrue) {
                fitting.add(s);
            }
        }
        return Aggregates.random(fitting.isEmpty() ? candidates : fitting);
    }
}
