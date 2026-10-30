package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.function.Predicate;

public class  DamageAllAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Card source = sa.getHostCard();

        // wait until stack is empty (prevents duplicate kills)
        if (!ai.getGame().getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.StackNotEmpty);
        }

        int x = -1;
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$Converge")) {
        	dmg = ComputerUtilMana.getConvergeCount(sa, ai);
        }
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            x = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
        }
        if (x == -1) {
            if (determineOppToKill(ai, sa, source, dmg) != null) {
                // we already know we can kill a player, so go for it
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            // look for other value in this (damaging creatures or
            // creatures + player, e.g. Pestilence, etc.)
             if (evaluateDamageAll(ai, sa, source, dmg) > 0) {
                 return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
             }
             return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else {
            int best = -1, best_x = -1;
            Player bestOpp = determineOppToKill(ai, sa, source, x);
            if (bestOpp != null) {
                // we can finish off a player, so go for it

                // TODO: improve this by possibly damaging more creatures
                // on the battlefield belonging to other opponents at the same
                // time, if viable
                best_x = bestOpp.getLife();
            } else {
                // see if it's possible to get value from killing off creatures
                for (int i = 0; i <= x; i++) {
                    final int value = evaluateDamageAll(ai, sa, source, i);
                    if (value > best) {
                        best = value;
                        best_x = i;
                    }
                }
            }

            if (best_x > 0) {
                if (sa.getSVar(damage).equals("Count$xPaid")) {
                    sa.setXManaCostPaid(best_x);
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    private Player determineOppToKill(Player ai, SpellAbility sa, Card source, int x) {
        // Attempt to determine which opponent can be finished off such that the most players
        // are killed at the same time, given X damage tops
        final String validP = sa.getParamOrDefault("ValidPlayers", "");
        Player bestOpp = null; // default opponent, if all else fails

        for (int dmg = 1; dmg <= x; dmg++) {
            // Don't kill yourself or a teammate in the process
            if (validP.equals("Player") && killsOwnSide(ai, dmg, source)) {
                break;
            }
            for (Player opp : ai.getOpponents()) {
                if ((validP.equals("Player") || validP.contains("Opponent"))
                        && (opp.getLife() <= ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
                    bestOpp = opp;
                }
            }
        }

        return bestOpp;
    }

    private int evaluateDamageAll(Player ai, SpellAbility sa, final Card source, int dmg) {
        final Player opp = ai.getWeakestOpponent();
        CardCollection humanList = getKillableCreatures(sa, ai.getOpponents(), dmg);
        CardCollection computerList = getKillableCreatures(sa, AiTeams.side(ai), dmg);
        int sides = AiTeams.opposingSides(ai);

        if (sa.usesTargeting() && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            humanList = getKillableCreatures(sa, opp, dmg);
            computerList.clear();
            sides = 1;
        }

        final String validP = sa.getParamOrDefault("ValidPlayers", "");
        // TODO: if damage is dependent on mana paid, maybe have X be human's max life
        // Don't kill yourself or a teammate
        if (validP.equals("Player") && killsOwnSide(ai, dmg, source)) {
            return -1;
        }

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts().isReusuableResource()) {
            if (computerList.isEmpty()) {
                minGain = 10; // nothing to lose
                // no creatures to lose and player can be damaged
                // so do it if it's helping!
                // ----------------------------
                // needs future improvement on pestilence :
                // what if we lose creatures but can win by repeated activations?
                // that tactic only works if there are creatures left to keep pestilence in play
                // and can kill the player in a reasonable amount of time (no more than 2-3 turns?)
                if (validP.equals("Player")) {
                    if (ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) > 0) {
                        // When using Pestilence to hurt players, do it at
                        // the end of the opponent's turn only
                        if (!"DmgAllCreaturesAndPlayers".equals(sa.getParam("AILogic"))
                                || (ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)
                                && !ai.getGame().getPhaseHandler().isPlayerTurn(ai)))
                        // Need further improvement : if able to kill immediately with repeated activations, do not wait
                        // for phases! Will also need to implement considering repeated activations for killed creatures!
                        // || (ai.sa.getPayCosts(). ??? )
                        {
                            // would take zero damage, and hurt opponent, do it!
                            if (ComputerUtilCombat.predictDamageTo(ai, dmg, source, false) < 1) {
                                return 1;
                            }
                            // enemy is expected to die faster than AI from damage if repeated
                            if (ai.getLife() > ComputerUtilCombat.predictDamageTo(ai, dmg, source, false)
                                    * ((opp.getLife() + ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) - 1)
                                    / ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
                                // enemy below 10 life, go for it!
                                if ((opp.getLife() < 10)
                                        && (ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) >= 1)) {
                                    return 1;
                                }
                                // At least half enemy remaining life can be removed in one go
                                // worth doing even if enemy still has high health - one more copy of spell to win!
                                if (opp.getLife() <= 2 * ComputerUtilCombat.predictDamageTo(opp, dmg, source, false)) {
                                    return 1;
                                }
                            }
                        }
                    }
                }
            } else {
                minGain = 100; // safety for errors in evaluate creature
            }
        } else if (sa.getSubAbility() != null && ai.getGame().getPhaseHandler().is(PhaseType.MAIN1) && computerList.isEmpty()
                && opp.getCreaturesInPlay().size() > 1 && !ai.getCreaturesInPlay().isEmpty()) {
            minGain = 126; // prepare for attack
        }

        return ComputerUtilCard.evaluateCreatureList(humanList) - ComputerUtilCard.evaluateCreatureList(computerList) * sides
                - minGain;
    }

    /** Whether dealing this much to each player kills the AI or a teammate (a Two-Headed Giant team takes it once per head). */
    private static boolean killsOwnSide(final Player ai, final int dmg, final Card source) {
        for (final Player p : AiTeams.side(ai)) {
            if (p.getLife() <= ComputerUtilCombat.predictDamageTo(p, dmg, source, false) * AiTeams.sharedLifeHits(p)
                    && !p.cantLoseForZeroOrLessLife()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String validP = sa.getParamOrDefault("ValidPlayers", "");

        final String damage = sa.getParam("NumDmg");
        int dmg;
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            dmg = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
        } else {
            dmg = AbilityUtils.calculateAmount(source, damage, sa);
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getWeakestOpponent();
        CardCollection humanList = getKillableCreatures(sa, ai.getOpponents(), dmg);
        CardCollection computerList = getKillableCreatures(sa, AiTeams.side(ai), dmg);

        if (sa.usesTargeting() && sa.canTarget(enemy)) {
            sa.resetTargets();
            sa.getTargets().add(enemy);
            humanList = getKillableCreatures(sa, enemy, dmg);
            computerList.clear();
        }
        // Don't get yourself or a teammate killed
        if (validP.equals("Player") && killsOwnSide(ai, dmg, source)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // if we can kill human, do it
        if ((validP.equals("Player") || validP.equals("Opponent") || validP.contains("Targeted"))
                && (enemy.getLife() <= ComputerUtilCombat.predictDamageTo(enemy, dmg, source, false))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (!computerList.isEmpty() && ComputerUtilCard.evaluateCreatureList(computerList) > ComputerUtilCard
                .evaluateCreatureList(humanList)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * getKillableCreatures.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.game.card.CardCollection} object.
     */
    private CardCollection getKillableCreatures(final SpellAbility sa, final Player player, final int dmg) {
        return getKillableCreatures(sa, new PlayerCollection(player), dmg);
    }

    private CardCollection getKillableCreatures(final SpellAbility sa, final PlayerCollection players, final int dmg) {
        final Card source = sa.getHostCard();
        String validC = sa.getParamOrDefault("ValidCards", "");

        // TODO: X may be something different than X paid
        CardCollection list =
                CardLists.getValidCards(players.getCardsIn(ZoneType.Battlefield), validC, source.getController(), source, sa);

        final Predicate<Card> filterKillable = c -> ComputerUtilCombat.predictDamageTo(c, dmg, source, false) >= ComputerUtilCombat.getDamageToKill(c, false);

        list = CardLists.getNotKeyword(list, Keyword.INDESTRUCTIBLE);
        list = CardLists.filter(list, filterKillable);

        return list;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final String validP = sa.getParamOrDefault("ValidPlayers", "");

        final String damage = sa.getParam("NumDmg");
        int dmg;

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid") && sa.getPayCosts().hasXInAnyCostPart()) {
            dmg = ComputerUtilCost.setMaxXValue(sa, ai, true);
        } else {
            dmg = AbilityUtils.calculateAmount(source, damage, sa);
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getWeakestOpponent();
        CardCollection humanList = getKillableCreatures(sa, ai.getOpponents(), dmg);
        CardCollection computerList = getKillableCreatures(sa, AiTeams.side(ai), dmg);

        if (sa.usesTargeting() && sa.canTarget(enemy)) {
            sa.resetTargets();
            sa.getTargets().add(enemy);
            humanList = getKillableCreatures(sa, enemy, dmg);
            computerList.clear();
        }

        // If it's not mandatory check a few things
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // Don't get yourself or a teammate killed
        if (validP.equals("Player") && killsOwnSide(ai, dmg, source)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // if we can kill human, do it
        if ((validP.equals("Player") || validP.contains("Opponent") || validP.contains("Targeted"))
                && (enemy.getLife() <= ComputerUtilCombat.predictDamageTo(enemy, dmg, source, false))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (!computerList.isEmpty() && ComputerUtilCard.evaluateCreatureList(computerList) + 50 >= ComputerUtilCard
                .evaluateCreatureList(humanList)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
