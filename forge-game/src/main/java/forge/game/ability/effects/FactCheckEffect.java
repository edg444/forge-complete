package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityChooseGuessOutcome;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Fact Checker: its controller looks at the top card of the Defined$ player's library and tells them its name; they
 * say its mana cost, type line, power, and toughness, picking each from the real one and some plausible others (user
 * ruling 2026-10-22; an AI gets everything right about half the time). Then the card is revealed, and the host's SVar
 * FactWrong is 1 if they got anything wrong.
 */
public class FactCheckEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Fact-check the top card of " + getDefinedPlayersOrTargeted(sa) + "'s library.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player checker = sa.getActivatingPlayer();
        final Game game = checker.getGame();
        host.setSVar("FactWrong", "Number$0");
        final List<Player> players = getDefinedPlayersOrTargeted(sa);
        if (players.isEmpty() || players.get(0).getCardsIn(ZoneType.Library).isEmpty()) {
            return;
        }
        final Player teller = players.get(0);
        final Card top = teller.getCardsIn(ZoneType.Library).get(0);
        checker.getController().reveal(new CardCollection(top), ZoneType.Library, teller, "Top card of " + teller + "'s library: ", false);
        game.getAction().notifyOfValue(sa, teller, "The top card of your library is " + top.getName() + ".", null);

        final Map<String, String> truth = new LinkedHashMap<>();
        truth.put("Mana cost", top.getManaCost().isNoCost() ? "(none)" : top.getManaCost().toString());
        truth.put("Type line", top.getType().toString());
        truth.put("Power", top.isCreature() ? String.valueOf(top.getNetPower()) : "(none)");
        truth.put("Toughness", top.isCreature() ? String.valueOf(top.getNetToughness()) : "(none)");

        boolean wrong = false;
        final Boolean outcome = StaticAbilityChooseGuessOutcome.chosenOutcome(teller, sa);
        if (outcome != null) {
            wrong = !outcome;
        } else if (teller.getController().isAI()) {
            wrong = MyRandom.getRandom().nextBoolean();
        } else {
            for (final Map.Entry<String, String> e : truth.entrySet()) {
                final List<String> options = options(e.getKey(), e.getValue());
                final String said = teller.getController().chooseSomeType(e.getKey().toLowerCase() + " of " + top.getName(), sa, options);
                if (!e.getValue().equals(said)) {
                    wrong = true;
                }
            }
        }
        game.getAction().reveal(new CardCollection(top), teller, false);
        game.getGameLog().add(GameLogEntryType.INFORMATION, teller + (wrong ? " got something wrong" : " got it all right")
                + " about " + top.getName() + ".");
        if (wrong) {
            GuessedWrongEffect.fire(teller);
            host.setSVar("FactWrong", "Number$1");
        }
    }

    private static List<String> options(final String part, final String actual) {
        final Set<String> out = new LinkedHashSet<>();
        out.add(actual);
        if ("Power".equals(part) || "Toughness".equals(part)) {
            out.add("(none)");
            for (int i = 0; i <= 8; i++) {
                out.add(String.valueOf(i));
            }
            return new ArrayList<>(out);
        }
        final List<PaperCard> pool = new ArrayList<>(StaticData.instance().getCommonCards().getUniqueCards());
        for (int tries = 0; out.size() < 5 && tries < 100; tries++) {
            final PaperCard pc = Aggregates.random(pool);
            out.add("Mana cost".equals(part)
                    ? (pc.getRules().getManaCost().isNoCost() ? "(none)" : pc.getRules().getManaCost().toString())
                    : pc.getRules().getType().toString());
        }
        final List<String> shuffled = new ArrayList<>(out);
        Collections.shuffle(shuffled, MyRandom.getRandom());
        return shuffled;
    }
}
