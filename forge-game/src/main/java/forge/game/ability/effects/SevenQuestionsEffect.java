package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

// Head to Head. The parenthetical on that card is rules text, not flavour: the questioned player
// looks at their top card, the asker puts up to six yes-or-no questions to them, and question seven
// is the guess at its name. Against a human the truthful answering is on their honour, but Forge
// knows exactly what the card is, so the questions here are ones the engine can answer itself -
// which makes the whole game playable against the AI rather than being an empty prompt.
public class SevenQuestionsEffect extends SpellAbilityEffect {

    private static final String GUESS_NOW = "Skip to the guess (question seven)";

    private static final Map<String, Predicate<Card>> QUESTIONS = Maps.newLinkedHashMap();
    static {
        QUESTIONS.put("Is it a land?", Card::isLand);
        QUESTIONS.put("Is it a creature?", Card::isCreature);
        QUESTIONS.put("Is it an instant or sorcery?", c -> c.isInstant() || c.isSorcery());
        QUESTIONS.put("Is it an artifact?", Card::isArtifact);
        QUESTIONS.put("Is it an enchantment?", Card::isEnchantment);
        QUESTIONS.put("Is it legendary?", c -> c.getType().isLegendary());
        QUESTIONS.put("Is it white?", Card::isWhite);
        QUESTIONS.put("Is it blue?", Card::isBlue);
        QUESTIONS.put("Is it black?", Card::isBlack);
        QUESTIONS.put("Is it red?", Card::isRed);
        QUESTIONS.put("Is it green?", Card::isGreen);
        QUESTIONS.put("Is it colorless?", Card::isColorless);
        QUESTIONS.put("Is it more than one color?", c -> c.getColor().countColors() > 1);
        QUESTIONS.put("Is its mana value 3 or greater?", c -> c.getCMC() >= 3);
        QUESTIONS.put("Is its mana value 5 or greater?", c -> c.getCMC() >= 5);
        QUESTIONS.put("Is its mana value 1 or less?", c -> c.getCMC() <= 1);
        QUESTIONS.put("Does its name begin with a letter from A to M?", c -> {
            final String n = c.getDisplayName().toUpperCase();
            return !n.isEmpty() && n.charAt(0) >= 'A' && n.charAt(0) <= 'M';
        });
        QUESTIONS.put("Is its name more than one word?", c -> c.getDisplayName().trim().contains(" "));
        QUESTIONS.put("Is its power 3 or greater?", c -> c.isCreature() && c.getNetPower() >= 3);
        QUESTIONS.put("Does it have flying?", c -> c.hasKeyword("Flying"));
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard().getName() + " - play Seven Questions.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player asker = sa.getActivatingPlayer();

        final List<Player> tgts = getDefinedPlayersOrTargeted(sa);
        if (tgts.isEmpty()) {
            return;
        }
        final Player owner = tgts.get(0);
        final Card top = owner.getCardsIn(ZoneType.Library).isEmpty() ? null
                : owner.getCardsIn(ZoneType.Library).get(0);
        if (top == null) {
            return;
        }

        // the questioned player looks at the card first, as the card instructs
        owner.getController().reveal(new forge.game.card.CardCollection(top), ZoneType.Library, owner,
                "Seven Questions - the top card of your library: ");

        final int allowed = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Questions", "6"), sa);
        final List<String> remaining = Lists.newArrayList(QUESTIONS.keySet());
        for (int i = 0; i < allowed && !remaining.isEmpty(); i++) {
            // the card allows UP TO six questions, so guessing early has to be an offered choice -
            // cancelling out of the prompt isn't reliably available
            final List<String> choices = Lists.newArrayList(remaining);
            choices.add(0, GUESS_NOW);
            final String question = asker.getController().chooseSomeType("Question", sa, choices, true);
            if (question == null || GUESS_NOW.equals(question)) {
                break;
            }
            remaining.remove(question);
            final boolean answer = QUESTIONS.get(question).test(top);
            host.getGame().getAction().notifyOfValue(sa, owner,
                    question + " " + (answer ? "Yes." : "No."), null);
        }

        final String guess = asker.getController().guessString(sa, "Question seven - name the card");
        // a flavor name is what the guesser would have seen, so either name counts
        final boolean correct = guess != null
                && (guess.trim().equalsIgnoreCase(top.getName().trim())
                        || guess.trim().equalsIgnoreCase(top.getDisplayName().trim()));

        host.getGame().getAction().notifyOfValue(sa, owner,
                (correct ? "Correct! " : "Wrong. ") + "The card was " + top.getDisplayName() + ".", null);

        if (correct && sa.hasParam("GuessCorrect")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessCorrect"));
        } else if (!correct && sa.hasParam("GuessWrong")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessWrong"));
        }
    }
}
