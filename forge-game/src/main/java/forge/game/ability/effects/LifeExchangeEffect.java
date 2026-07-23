package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LifeExchangeEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Player activatingPlayer = sa.getActivatingPlayer();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("TeamExchange") && !tgtPlayers.isEmpty()) {
            sb.append(Lang.joinHomogenous(getExchangeGroup(tgtPlayers.get(0))));
            sb.append(" exchange life totals.");
            return sb.toString();
        }

        if (tgtPlayers.size() == 1) {
            sb.append(activatingPlayer).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(0));
        } else if (tgtPlayers.size() > 1) {
            sb.append(tgtPlayers.get(0)).append(" exchanges life totals with ");
            sb.append(tgtPlayers.get(1));
        }
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();

        if (sa.hasParam("TeamExchange")) {
            resolveTeamExchange(sa);
            return;
        }

        Player p1;
        Player p2;

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() == 1) {
            p1 = sa.getActivatingPlayer();
            p2 = tgtPlayers.get(0);
        } else {
            p1 = tgtPlayers.get(0);
            p2 = tgtPlayers.get(1);
        }

        final int life1 = p1.getLife();
        final int life2 = p2.getLife();
        final int diff = Math.abs(life1 - life2);

        if (life2 > life1) {
            // swap players
            Player tmp = p2;
            p2 = p1;
            p1 = tmp;
        }
        if (diff > 0 && p1.canLoseLife() && p2.canGainLife()) {
            final int lost = p1.loseLife(diff, false, false);
            p2.gainLife(diff, source, sa);
            if (lost > 0) {
                final Map<Player, Integer> lossMap = Maps.newHashMap();
                lossMap.put(p1, lost);
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPIMap(lossMap);
                source.getGame().getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runParams, false);
                if (sa.hasParam("RememberOwnLoss") && p1.equals(sa.getActivatingPlayer())) {
                    source.addRemembered(lost);
                }
            }
        }
        if (sa.hasParam("RememberDifference")) {
            source.addRemembered(p1.getLife() - p2.getLife());
        }
    }

    // "Get a Life"-style N-way exchange: target player and each of their teammates. There's no
    // official ruling for exchanging more than 2 life totals at once (this text is unique to a
    // silver-bordered card), so this generalizes the standard pairwise swap into a cyclic
    // rotation - each player's new life total is the next player's old one. For the only team
    // size Forge actually supports (two-player teams) this is identical to a normal exchange.
    private static List<Player> getExchangeGroup(Player target) {
        final List<Player> group = new ArrayList<>();
        for (Player p : target.getGame().getPlayers()) {
            if (p.isInGame() && p.sameTeam(target)) {
                group.add(p);
            }
        }
        return group;
    }

    private void resolveTeamExchange(SpellAbility sa) {
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        if (tgtPlayers.isEmpty()) {
            return;
        }
        final List<Player> group = getExchangeGroup(tgtPlayers.get(0));
        if (group.size() < 2) {
            return;
        }

        final int[] lifeTotals = new int[group.size()];
        for (int i = 0; i < group.size(); i++) {
            lifeTotals[i] = group.get(i).getLife();
        }
        for (int i = 0; i < group.size(); i++) {
            final Player p = group.get(i);
            final int newLife = lifeTotals[(i + 1) % group.size()];
            final int diff = newLife - p.getLife();
            if (diff > 0 && p.canGainLife()) {
                p.gainLife(diff, sa.getHostCard(), sa);
            } else if (diff < 0 && p.canLoseLife()) {
                p.loseLife(-diff, false, false);
            }
        }
    }
}
