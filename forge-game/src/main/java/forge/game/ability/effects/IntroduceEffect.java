package forge.game.ability.effects;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * In Residence: "you may introduce the exiled card using an introduction you haven't used this game." A person types
 * the introduction (honor system) and one they've used this game is refused; the AI says its stock lines, each once
 * (user ruling 2026-10-22). If the card was introduced, IntroducedSubAbility$ resolves.
 */
public class IntroduceEffect extends SpellAbilityEffect {

    private static final List<String> AI_INTRODUCTIONS = List.of(
            "Ladies and gentlemen, put your hands together for %s!",
            "Making their long-awaited debut: %s!",
            "You know them, you love them: %s!",
            "Straight from the top of the bill, it's %s!",
            "Please give a warm welcome to %s!",
            "And now, the moment you've all been waiting for: %s!",
            "One night only: %s!",
            "Back by popular demand, %s!");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> usedBy(final Player p) {
        final String key = "IntroductionsUsed";
        Object used = p.getGameNote(key);
        if (!(used instanceof Set<?>)) {
            used = new HashSet<String>();
            p.setGameNote(key, used);
        }
        return (Set<String>) used;
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParamOrDefault("Defined", "Remembered"), sa);
        if (cards.isEmpty()) {
            return;
        }
        final String name = cards.get(0).getName();
        final Set<String> used = usedBy(p);
        String introduction = null;
        if (p.getController().isAI()) {
            for (final String line : AI_INTRODUCTIONS) {
                if (!used.contains(line.toLowerCase(Locale.ROOT))) {
                    introduction = line;
                    break;
                }
            }
        } else if (p.getController().confirmAction(sa, null, "Introduce " + name + "?", null)) {
            for (int attempt = 0; attempt < 10 && introduction == null; attempt++) {
                final String entered = p.getController().guessString(sa, "Introduce " + name
                        + (used.isEmpty() ? "" : " (an introduction you haven't used this game)"));
                if (entered == null || entered.trim().isEmpty()) {
                    break;
                }
                if (!used.contains(entered.trim().toLowerCase(Locale.ROOT))) {
                    introduction = entered.trim();
                }
            }
        }
        if (introduction == null) {
            return;
        }
        used.add(introduction.toLowerCase(Locale.ROOT));
        final String said = introduction.contains("%s") ? String.format(introduction, name) : introduction;
        p.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, p + ": \"" + said + "\"");
        p.getGame().getAction().notifyOfValue(sa, p, said, p);
        final SpellAbility then = sa.getAdditionalAbility("IntroducedSubAbility");
        if (then != null) {
            AbilityUtils.resolve(then);
        }
    }
}
