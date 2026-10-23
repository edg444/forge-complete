package forge.game.ability.effects;

import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

/**
 * A real-world number a card asks about (Pin Collector's Booth's pins, New Player's Journey's years played): a person
 * says it once per match (honor system), the AI's is random between AIMin$ and AIMax$, also once per match (user
 * rulings 2026-10-22). Count$MatchNumber.Key reads it back.
 */
public class MatchNumberEffect extends SpellAbilityEffect {

    public static String noteKey(final String key) {
        return "MatchNumber:" + key;
    }

    /** The player's number for the key, or 0 if they were never asked. */
    public static int numberOf(final Player p, final String key) {
        return p.getMatchNote(noteKey(key)) instanceof Integer n ? n : 0;
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final String key = sa.getParam("Key");
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (p.getMatchNote(noteKey(key)) != null) {
                continue;
            }
            final int max = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Max", "99"), sa);
            final int n;
            if (p.getController().isAI()) {
                final int lo = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("AIMin", "0"), sa);
                final int hi = Math.max(lo, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("AIMax", "0"), sa));
                n = lo + MyRandom.getRandom().nextInt(hi - lo + 1);
            } else {
                n = p.getController().chooseNumber(sa, sa.getParamOrDefault("Prompt", key), 0, max);
            }
            p.setMatchNote(noteKey(key), n);
            p.getGame().getGameLog().add(GameLogEntryType.INFORMATION, p + ": " + sa.getParamOrDefault("LogText", key) + " " + n + ".");
        }
    }
}
