package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * "Score one point for your team." The match keeps a scoreboard of the Mirran and Phyrexian teams' points (user ruling
 * 2026-10-21); a player not on a team yet joins one first.
 */
public class ScorePointEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Score one point for your team.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final int n = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "1"), sa);
        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final String team = ChooseTeamEffect.chooseIfNeeded(p, sa);
            final String key = ChooseTeamEffect.POINTS_NOTE + team;
            final int mine = p.getMatchNote(key) instanceof Integer i ? i : 0;
            p.setMatchNote(key, mine + n);
            final String other = ChooseTeamEffect.MIRRAN.equals(team) ? ChooseTeamEffect.PHYREXIAN : ChooseTeamEffect.MIRRAN;
            p.getGame().getAction().notifyOfValue(sa, p, p + " scores " + n + " for the " + team + " team. Scoreboard: "
                    + team + " " + ChooseTeamEffect.teamPoints(p, team) + ", " + other + " "
                    + ChooseTeamEffect.teamPoints(p, other) + ".", null);
        }
    }
}
