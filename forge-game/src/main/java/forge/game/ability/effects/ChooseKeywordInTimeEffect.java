package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.keyword.PrintedCreatureKeywords;
import forge.game.player.KeywordDayLog;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Modular Monstrosity: "you have five seconds to choose a keyword you haven't chosen for a card named Modular Monstrosity
 * today that's been printed on a creature card. If you do, this creature gains that ability. Otherwise, this creature
 * loses all keyword abilities." Rulings: any keyword on a printed creature card, with a printed modifier; each keyword
 * once a day across every Monstrosity, whatever the modifier; no valid choice in time and it loses every keyword
 * ability, ones gained other ways included. The clock is the real one (Hot Fix's idiom).
 */
public class ChooseKeywordInTimeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " has five seconds to choose a keyword.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = you.getGame();
        final int seconds = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Seconds", "5"), sa);

        final Set<String> usedToday = KeywordDayLog.chosenToday(you);
        final List<String> options = new ArrayList<>();
        for (final String k : PrintedCreatureKeywords.all()) {
            if (!usedToday.contains(PrintedCreatureKeywords.keyword(k).name())) {
                options.add(k);
            }
        }

        final String chosen = options.isEmpty() ? null : you.getController().chooseKeywordInTime(options, seconds, sa);
        final long ts = game.getNextTimestamp();
        if (chosen != null && options.contains(chosen)) {
            KeywordDayLog.record(you, PrintedCreatureKeywords.keyword(chosen).name());
            if (host.isInPlay()) {
                host.addChangedCardKeywords(new ArrayList<>(List.of(chosen)), new ArrayList<>(), false, ts, null);
            }
            game.getGameLog().add(GameLogEntryType.INFORMATION, you + " chose " + PrintedCreatureKeywords.title(chosen)
                    + " for " + host + ".");
        } else {
            if (host.isInPlay()) {
                host.addChangedCardKeywords(new ArrayList<>(), new ArrayList<>(), true, ts, null);
            }
            game.getGameLog().add(GameLogEntryType.INFORMATION, you + " didn't choose a keyword in time, so " + host
                    + " loses all keyword abilities.");
        }
        game.fireEvent(new forge.game.event.GameEventCardStatsChanged(host));
    }
}
