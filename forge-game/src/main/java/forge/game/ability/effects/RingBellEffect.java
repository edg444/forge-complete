package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Rings a Bell: "After you say the chosen word for the first time each turn, an opponent may ring or imitate a bell
 * within five seconds." Saying the word is the special action this resolves from; the opponents then share one real
 * five-second window (Hot Fix's clock idiom). Opponents who answer at once (the AI, AIChance$) go first, so a person is
 * only asked when no one has rung yet. With RememberRingers$, whoever rang is remembered on the card.
 */
public class RingBellEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " says the chosen word.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player sayer = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = sayer.getGame();
        final String word = host.getChosenType();
        if (word == null || word.isEmpty()) {
            return;
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, sayer + " says \"" + word + "\".");

        final int seconds = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Seconds", "5"), sa);
        final long deadline = System.currentTimeMillis() + seconds * 1000L;
        final List<Player> listeners = new ArrayList<>();
        for (final Player p : sayer.getOpponents()) {
            if (p.getController().isAI()) {
                listeners.add(0, p);
            } else {
                listeners.add(p);
            }
        }
        for (final Player p : listeners) {
            final int left = (int) Math.ceil((deadline - System.currentTimeMillis()) / 1000.0);
            if (left <= 0) {
                break;
            }
            if (p.getController().ringBellInTime(sayer, word, left, sa)) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, p + " rings a bell.");
                if (sa.hasParam("RememberRingers")) {
                    host.addRemembered(p);
                }
                return;
            }
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, "No opponent rang a bell in time.");
    }
}
