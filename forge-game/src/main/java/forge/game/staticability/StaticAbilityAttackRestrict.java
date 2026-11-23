package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class StaticAbilityAttackRestrict {

    static public Integer globalAttackRestrict(Game game) {
        Integer max = null;
        for (final StaticAbility stAb : attackRestrictSources(game, null)) {
            int stMax = maxAttackers(stAb);
            if (null == max || stMax < max) {
                max = stMax;
            }
        }
        return max;
    }

    /** Rule with an Even Hand: "You can't attack with an odd number of creatures." */
    static public boolean evenAttackersOnly(final forge.game.player.Player attacker) {
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.AttackRestrict) && stAb.hasParam("EvenAttackers")
                        && stAb.matchesValidParam("ValidPlayer", attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    static public Integer attackRestrictNum(GameEntity defender) {
        Integer num = null;
        for (final StaticAbility stAb : attackRestrictSources(defender.getGame(), defender)) {
            int stNum = maxAttackers(stAb);
            if (null == num || stNum < num) {
                num = stNum;
            }
        }
        return num;
    }

    static public int maxAttackers(StaticAbility stAb) {
        return AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParamOrDefault("MaxAttackers", "1"), stAb);
    }

    /**
     * @return the AttackRestrict static abilities limiting the number of attackers,
     *         either overall (defender == null) or against the given defender
     */
    static public List<StaticAbility> attackRestrictSources(Game game, GameEntity defender) {
        final List<StaticAbility> result = new ArrayList<>();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRestrict)
                        || stAb.hasParam("ValidDefender") != (defender != null) || stAb.hasParam("EvenAttackers")) {
                    continue;
                }
                if (defender == null || attackRestrict(stAb, defender)) {
                    result.add(stAb);
                }
            }
        }
        return result;
    }

    static public boolean attackRestrict(StaticAbility stAb, GameEntity defender) {
        if (!stAb.matchesValidParam("ValidDefender", defender)) {
            return false;
        }
        return true;
    }
}
