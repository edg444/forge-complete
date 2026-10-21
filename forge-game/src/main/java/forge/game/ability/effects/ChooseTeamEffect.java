package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Unknown Event's teams. A player who isn't on the Mirran or the Phyrexian team yet chooses one the first time a card
 * asks, and stays on it for the rest of the match - every card asks about the same team (user ruling 2026-10-21). The
 * AI joins one at random.
 */
public class ChooseTeamEffect extends SpellAbilityEffect {

    public static final String MIRRAN = "Mirran";
    public static final String PHYREXIAN = "Phyrexian";
    public static final String NOTE = "UnknownEventTeam";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            chooseIfNeeded(p, sa);
        }
    }

    /** The player's team, chosen now if they haven't joined one yet this match. */
    public static String chooseIfNeeded(final Player p, final SpellAbility sa) {
        final Object known = p.getMatchNote(NOTE);
        if (known instanceof String team) {
            return team;
        }
        final List<String> teams = List.of(MIRRAN, PHYREXIAN);
        String team = p.getController().isAI() ? Aggregates.random(teams)
                : p.getController().chooseSomeType("team to join for the rest of the match", sa, teams);
        if (team == null) {
            team = teams.get(0);
        }
        p.setMatchNote(NOTE, team);
        p.getGame().getAction().notifyOfValue(sa, p, p + " joins the " + team + " team for the rest of the match.", null);
        return team;
    }
}
