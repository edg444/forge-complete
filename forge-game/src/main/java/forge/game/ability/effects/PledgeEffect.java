package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import forge.card.ColorSet;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Pledge (Guildmark): join a two-colored guild if you haven't already this game. The guild is the player's for the
 * rest of the game; "your guild's colors" are its two. The AI joins the guild of its deck's two most prominent colors.
 */
public class PledgeEffect extends SpellAbilityEffect {

    /** The ten Ravnica guilds and their colors. */
    public static final Map<String, String> GUILDS = new java.util.LinkedHashMap<>();
    static {
        GUILDS.put("Azorius", "white blue");
        GUILDS.put("Dimir", "blue black");
        GUILDS.put("Rakdos", "black red");
        GUILDS.put("Gruul", "red green");
        GUILDS.put("Selesnya", "green white");
        GUILDS.put("Orzhov", "white black");
        GUILDS.put("Izzet", "blue red");
        GUILDS.put("Golgari", "black green");
        GUILDS.put("Boros", "red white");
        GUILDS.put("Simic", "green blue");
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Join a two-colored guild if you haven't already this game.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (p.getGuild() != null) {
                continue;
            }
            final List<String> guilds = List.copyOf(GUILDS.keySet());
            String guild;
            if (p.getController().isAI()) {
                guild = guildForDeck(p);
            } else {
                guild = p.getController().chooseSomeType("guild to join for the rest of the game", sa, guilds);
            }
            if (guild == null) {
                guild = guilds.get(0);
            }
            p.setGuild(guild);
            p.getGame().getAction().notifyOfValue(sa, p, p + " joins the " + guild + ".", null);
        }
    }

    /** The guild whose two colors the most of the player's cards share. */
    private static String guildForDeck(final Player p) {
        String best = null;
        int bestCount = -1;
        for (final Map.Entry<String, String> e : GUILDS.entrySet()) {
            final ColorSet colors = ColorSet.fromNames(e.getValue().split(" "));
            int count = 0;
            for (final Card c : p.getAllCards()) {
                count += c.getColor().hasAnyColor(colors.getColor()) ? 1 : 0;
            }
            if (count > bestCount) {
                bestCount = count;
                best = e.getKey();
            }
        }
        return best;
    }
}
