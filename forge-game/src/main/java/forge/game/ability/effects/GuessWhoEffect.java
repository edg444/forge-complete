package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

/**
 * Guess Who it Is!: look at the top ten cards of target opponent's library and choose a creature card among them. That
 * opponent asks five yes or no questions about it, answered truthfully, then names a card. If the name is wrong, the
 * creature enters under your control; everything else is shuffled back. A person asks from a menu of questions about
 * the card's characteristics and names it from card-name search; the AI asks the question that best splits the
 * creatures in its library still possible, then names the likeliest (user ruling 2026-10-22).
 */
public class GuessWhoEffect extends SpellAbilityEffect {

    private static final int LOOK = 10;
    private static final int QUESTIONS = 5;

    private record Question(String text, Predicate<Card> test) {}

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Look at the top ten cards of target opponent's library and choose a creature card from among them.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        for (final Player opp : getTargetPlayers(sa)) {
            final CardCollection top = new CardCollection(Lists.newArrayList(opp.getCardsIn(ZoneType.Library)).subList(0,
                    Math.min(LOOK, opp.getCardsIn(ZoneType.Library).size())));
            final CardCollection creatures = CardLists.filter(top, CardPredicates.CREATURES);
            Card chosen = null;
            if (!creatures.isEmpty()) {
                you.getController().reveal(top, ZoneType.Library, opp, "Top ten cards of " + opp + "'s library", false);
                chosen = you.getController().chooseSingleEntityForEffect(creatures, sa, "Choose a creature card", false, null);
            }
            if (chosen != null) {
                final List<Question> menu = questions(opp, chosen);
                // the cards the AI knows it could have: the creatures in its own library
                CardCollection possible = CardLists.filter(opp.getCardsIn(ZoneType.Library), CardPredicates.CREATURES);
                for (int i = 1; i <= QUESTIONS && !menu.isEmpty(); i++) {
                    final Question q;
                    if (opp.getController().isAI()) {
                        q = bestSplit(menu, possible);
                    } else {
                        final Map<String, Question> byText = Maps.newLinkedHashMap();
                        for (final Question each : menu) {
                            byText.put(each.text(), each);
                        }
                        q = byText.get(opp.getController().chooseSomeType("Question", sa, byText.keySet()));
                    }
                    if (q == null) {
                        break;
                    }
                    menu.remove(q);
                    final boolean yes = q.test().test(chosen);
                    possible = CardLists.filter(possible, c -> q.test().test(c) == yes);
                    game.getAction().notifyOfValue(sa, opp, "Question " + i + ": " + q.text() + " " + (yes ? "Yes." : "No."), null);
                }
                final String guess;
                // The Fact Checker: the guesser just chooses how it comes out
                final Boolean outcome = forge.game.staticability.StaticAbilityChooseGuessOutcome.chosenOutcome(opp, sa);
                if (outcome != null) {
                    guess = outcome ? chosen.getName() : "";
                } else if (opp.getController().isAI()) {
                    guess = possible.isEmpty() ? "" : likeliest(possible);
                } else {
                    guess = opp.getController().chooseCardName(sa, f -> true, "Card", "Name the chosen creature card");
                }
                game.getAction().reveal(new CardCollection(chosen), opp, false, opp + " guessed " + guess + ". The chosen card is ");
                game.getGameLog().add(GameLogEntryType.INFORMATION, opp + " guessed " + guess + "; it was " + chosen.getName() + ".");
                if (guess == null || !guess.equals(chosen.getName())) {
                    GuessedWrongEffect.fire(opp);
                    final Map<forge.game.ability.AbilityKey, Object> params = forge.game.ability.AbilityKey.newMap();
                    game.getAction().moveToPlay(chosen, you, sa, params);
                }
            }
            opp.shuffle(sa);
        }
    }

    /** Questions about printed characteristics, only those the cards in question could differ on. */
    private static List<Question> questions(final Player opp, final Card chosen) {
        final List<Question> all = Lists.newArrayList();
        for (final String color : MagicColor.Constant.ONLY_COLORS) {
            final byte mask = MagicColor.fromName(color);
            all.add(new Question("Is it " + color + "?", c -> c.getColor().hasAnyColor(mask)));
        }
        all.add(new Question("Is it colorless?", c -> c.getColor().isColorless()));
        all.add(new Question("Is it multicolored?", c -> c.getColor().isMulticolor()));
        all.add(new Question("Is it legendary?", c -> c.getType().isLegendary()));
        all.add(new Question("Is it an artifact?", Card::isArtifact));
        all.add(new Question("Is it an enchantment?", Card::isEnchantment));
        for (int n = 1; n <= 8; n++) {
            final int v = n;
            all.add(new Question("Is its mana value " + n + " or greater?", c -> c.getCMC() >= v));
        }
        for (int n = 1; n <= 7; n++) {
            final int v = n;
            all.add(new Question("Is its power " + n + " or greater?", c -> c.getNetPower() >= v));
            all.add(new Question("Is its toughness " + n + " or greater?", c -> c.getNetToughness() >= v));
        }
        final java.util.Set<String> types = new java.util.TreeSet<>();
        final java.util.Set<Keyword> keywords = new java.util.TreeSet<>();
        final CardCollection pool = CardLists.filter(opp.getCardsIn(ZoneType.Library), CardPredicates.CREATURES);
        pool.add(chosen);
        for (final Card c : pool) {
            if (!c.getType().hasAllCreatureTypes()) {
                c.getType().getCreatureTypes().forEach(types::add);
            }
            for (final Keyword k : Keyword.values()) {
                if (k != Keyword.UNDEFINED && c.hasKeyword(k)) {
                    keywords.add(k);
                }
            }
        }
        for (final String type : types) {
            all.add(new Question("Is it " + (forge.util.Lang.startsWithVowel(type) ? "an " : "a ") + type + "?", c -> c.getType().hasCreatureType(type)));
        }
        for (final Keyword k : keywords) {
            all.add(new Question("Does it have " + k.toString().toLowerCase(java.util.Locale.ROOT) + "?", c -> c.hasKeyword(k)));
        }
        return all;
    }

    private static Question bestSplit(final List<Question> menu, final CardCollection possible) {
        Question best = null;
        int bestScore = Integer.MAX_VALUE;
        for (final Question q : menu) {
            final int yes = CardLists.count(possible, q.test());
            final int score = Math.abs(2 * yes - possible.size());
            if (score < bestScore) {
                bestScore = score;
                best = q;
            }
        }
        return best;
    }

    /** The name the most of the remaining possible cards share, a random one on a tie. */
    private static String likeliest(final CardCollection possible) {
        final Map<String, Integer> counts = Maps.newHashMap();
        for (final Card c : possible) {
            counts.merge(c.getName(), 1, Integer::sum);
        }
        final int most = counts.values().stream().max(Integer::compare).orElse(0);
        return Aggregates.random(counts.entrySet().stream().filter(e -> e.getValue() == most).map(Map.Entry::getKey).toList());
    }
}
