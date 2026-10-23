/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * AbilityFactory_Turns class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AddTurnAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        Player opp = targetableOpps.min(PlayerPredicates.compareByLife());

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(ai) && (mandatory || !ai.getGame().getReplacementHandler().wouldExtraTurnBeSkipped(ai))) {
                sa.getTargets().add(ai);
            } else if (mandatory) {
                for (final Player ally : ai.getAllies()) {
                    if (sa.canTarget(ally)) {
                        sa.getTargets().add(ally);
                        break;
                    }
                }
                if (!sa.getTargetRestrictions().isMinTargetsChosen(sa.getHostCard(), sa) && opp != null) {
                    sa.getTargets().add(opp);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        } else {
            final List<Player> tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
            // TODO: improve ai for Sage of Hours
            if (!StringUtils.isNumeric(sa.getParam("NumTurns"))) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        if (losesAtTheEnd(sa) && !lethalNextTurn(aiPlayer)) {
            // Alchemist's Gambit without cleave: the extra turn ends in a loss, so it has to win the game first
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return doTriggerNoCost(aiPlayer, sa, false);
    }

    // follows the SVars the extra turn's delayed trigger sets up, looking for a LosesGame
    private static boolean losesAtTheEnd(final SpellAbility sa) {
        final Deque<String> names = new ArrayDeque<>();
        final Set<String> seen = new HashSet<>();
        if (sa.hasParam("ExtraTurnDelayedTriggerExecute")) {
            names.add(sa.getParam("ExtraTurnDelayedTriggerExecute"));
        }
        while (!names.isEmpty()) {
            final String name = names.pop();
            if (!seen.add(name) || !sa.hasSVar(name)) {
                continue;
            }
            final String svar = sa.getSVar(name);
            if (svar.contains("LosesGame")) {
                return true;
            }
            for (final String part : svar.split("\\|")) {
                final String[] kv = part.trim().split("\\$", 2);
                if (kv.length == 2 && (kv[0].equals("Triggers") || kv[0].equals("Execute") || kv[0].equals("SubAbility"))) {
                    names.addAll(Arrays.asList(kv[1].trim().split(",")));
                }
            }
        }
        return false;
    }

    // whether the AI's creatures could deal lethal damage on an extra turn, one blocker stopping one attacker
    private static boolean lethalNextTurn(final Player ai) {
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !opp.canLoseLife()) {
            return false;
        }
        final List<Card> attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> !c.hasKeyword(Keyword.DEFENDER));
        attackers.sort(Comparator.comparingInt((Card c) -> ComputerUtilCombat.damageIfUnblocked(c, opp, null, false)).reversed());
        for (final Card blocker : opp.getCreaturesInPlay()) {
            for (final Card attacker : attackers) {
                if (CombatUtil.canBlock(attacker, blocker, true)) {
                    attackers.remove(attacker);
                    break;
                }
            }
        }
        return ComputerUtilCombat.sumDamageIfUnblocked(attackers, opp) >= opp.getLife();
    }

}
