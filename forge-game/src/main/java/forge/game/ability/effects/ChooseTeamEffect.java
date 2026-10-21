package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Unknown Event's teams: Mirran or Phyrexian, and the race teams Cloudspire or Speed Demons (Teams$ Race). A player
 * joins one of each the first time a card asks - as the game begins when their deck has a card that cares (SVar
 * AsksTeam), else when a card resolves - and stays on it for the rest of the match; every card reads the same team
 * (user rulings 2026-10-21). The AI joins at random. Team$ joins that team if the player has none yet (a mode that is
 * only for one team).
 */
public class ChooseTeamEffect extends SpellAbilityEffect {

    public static final String MIRRAN = "Mirran";
    public static final String PHYREXIAN = "Phyrexian";
    public static final String CLOUDSPIRE = "Cloudspire";
    public static final String SPEED_DEMONS = "Speed Demons";
    public static final String NOTE = "UnknownEventTeam";
    public static final String RACE_NOTE = "UnknownEventRaceTeam";

    /** Points scored for each team this match are kept per player under this prefix (ScorePoint). */
    public static final String POINTS_NOTE = "TeamPoints:";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final boolean race = "Race".equals(sa.getParam("Teams"));
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if (sa.hasParam("Team") && teamOf(p, race) == null) {
                join(p, race, sa.getParam("Team"), sa);
            } else {
                chooseIfNeeded(p, sa, race);
            }
        }
    }

    public static String chooseIfNeeded(final Player p, final SpellAbility sa) {
        return chooseIfNeeded(p, sa, false);
    }

    /** The player's team, chosen now if they haven't joined one yet this match. */
    public static String chooseIfNeeded(final Player p, final SpellAbility sa, final boolean race) {
        final String known = teamOf(p, race);
        if (known != null) {
            return known;
        }
        final List<String> teams = race ? List.of(CLOUDSPIRE, SPEED_DEMONS) : List.of(MIRRAN, PHYREXIAN);
        String team = p.getController().isAI() ? Aggregates.random(teams)
                : p.getController().chooseSomeType((race ? "race team" : "team") + " to join for the rest of the match", sa, teams);
        if (team == null) {
            team = teams.get(0);
        }
        join(p, race, team, sa);
        return team;
    }

    public static String teamOf(final Player p, final boolean race) {
        return p.getMatchNote(race ? RACE_NOTE : NOTE) instanceof String team ? team : null;
    }

    private static void join(final Player p, final boolean race, final String team, final SpellAbility sa) {
        p.setMatchNote(race ? RACE_NOTE : NOTE, team);
        p.getGame().getAction().notifyOfValue(sa, p, p + " joins " + (race ? "Team " : "the ") + team
                + (race ? "" : " team") + " for the rest of the match.", null);
    }

    /** As the game begins: a player whose deck has a card that cares about a team (SVar AsksTeam) joins one now. */
    public static void chooseForDeck(final Player p, final Iterable<Card> cards) {
        Card team = null, race = null;
        for (final Card c : cards) {
            if (c.hasSVar("AsksTeam")) {
                team = team == null && c.getSVar("AsksTeam").contains("Team") ? c : team;
                race = race == null && c.getSVar("AsksTeam").contains("Race") ? c : race;
            }
        }
        if (team != null) {
            chooseIfNeeded(p, new SpellAbility.EmptySa(ApiType.ChooseTeam, team, p), false);
        }
        if (race != null) {
            chooseIfNeeded(p, new SpellAbility.EmptySa(ApiType.ChooseTeam, race, p), true);
        }
    }

    /** The points scored for a team so far this match, by every player in the game. */
    public static int teamPoints(final Player any, final String team) {
        int total = 0;
        for (final Player p : any.getGame().getRegisteredPlayers()) {
            if (p.getMatchNote(POINTS_NOTE + team) instanceof Integer n) {
                total += n;
            }
        }
        return total;
    }

    /** Underdog Racer: on a team with fewer points than the other team (ties aren't behind; user ruling 2026-10-21). */
    public static boolean isBehind(final Player p) {
        final String team = teamOf(p, false);
        if (team == null) {
            return false;
        }
        final String other = MIRRAN.equals(team) ? PHYREXIAN : MIRRAN;
        return teamPoints(p, team) < teamPoints(p, other);
    }
}
