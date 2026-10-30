package forge.ai.ability;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostDamage;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.function.Predicate;

public class DestroyAllAi extends SpellAbilityAi {

    private static final Predicate<Card> predicate = c -> !(c.hasKeyword(Keyword.INDESTRUCTIBLE) || c.getCounters(CounterEnumType.SHIELD) > 0 || c.hasSVar("SacMe"));

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return doMassRemovalLogic(ai, sa);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return doMassRemovalLogic(aiPlayer, sa);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        if ("FellTheMighty".equals(aiLogic)) {
            return SpecialCardAi.FellTheMighty.consider(ai, sa);
        }

        return doMassRemovalLogic(ai, sa);
    }

    public static AiAbilityDecision doMassRemovalLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("Always")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        String valid = sa.getParamOrDefault("ValidCards", "");

        if (valid.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
        }

        // teammates lose what the AI loses, and every opponent's board counts, not just the first one's
        CardCollection opplist;
        CardCollection ailist;
        PlayerCollection opponents;
        int sides;
        if (sa.usesTargeting()) {
            // aim at the opponent with the most to lose; nothing on the AI's side is hit
            Player best = null;
            CardCollection bestList = null;
            for (Player opponent : ai.getOpponents()) {
                if (!sa.canTarget(opponent)) {
                    continue;
                }
                CardCollection list = CardLists.filter(CardLists.getValidCards(opponent.getCardsIn(ZoneType.Battlefield),
                        valid, source.getController(), source, sa), predicate);
                if (!list.isEmpty() && (bestList == null
                        || ComputerUtilCard.evaluatePermanentList(list) > ComputerUtilCard.evaluatePermanentList(bestList))) {
                    best = opponent;
                    bestList = list;
                }
            }
            if (best == null) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            sa.resetTargets();
            sa.getTargets().add(best);
            opplist = bestList;
            ailist = new CardCollection();
            opponents = new PlayerCollection(best);
            sides = 1;
        } else {
            opplist = CardLists.filter(AiTeams.opponentCards(ai, ZoneType.Battlefield, valid, sa), predicate);
            ailist = CardLists.filter(AiTeams.sideCards(ai, ZoneType.Battlefield, valid, sa), predicate);
            opponents = ai.getOpponents();
            sides = AiTeams.opposingSides(ai);
        }
        if (opplist.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final int CREATURE_EVAL_THRESHOLD = 200;
        final Combat currentCombat = ai.getGame().getCombat();
        final boolean blockersDeclared = ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) && currentCombat != null;
        final PlayerCollection side = AiTeams.side(ai);

        // Special handling for Raiding Party
        if (logic.equals("RaidingParty")) {
            int numAiCanSave = Math.min(CardLists.count(side.getCreaturesInPlay(), CardPredicates.isColor(MagicColor.WHITE).and(CardPredicates.UNTAPPED)) * 2, ailist.size());
            int numOppsCanSave = Math.min(CardLists.count(ai.getOpponents().getCreaturesInPlay(), CardPredicates.isColor(MagicColor.WHITE).and(CardPredicates.UNTAPPED)) * 2, opplist.size());

            if (numOppsCanSave < opplist.size() && (ailist.size() - numAiCanSave < opplist.size() - numOppsCanSave)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else if (numAiCanSave < ailist.size() && (opplist.size() - numOppsCanSave < ailist.size() - numAiCanSave)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        // If effect is destroying creatures and AI (or a teammate) is about to lose, activate effect anyway no matter what!
        if (!CardLists.getType(opplist, "Creature").isEmpty() && blockersDeclared
                && side.anyMatch(p -> ComputerUtilCombat.lifeInSeriousDanger(p, currentCombat))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // If effect is destroying creatures and AI is about to get low on life, activate effect anyway if difference in lost permanents not very much
        if (!CardLists.getType(opplist, "Creature").isEmpty() && blockersDeclared
                && side.anyMatch(p -> ComputerUtilCombat.lifeInDanger(p, currentCombat))
                && ComputerUtilCard.evaluatePermanentList(ailist) * sides - 6 >= ComputerUtilCard.evaluatePermanentList(opplist)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
        if (CardLists.getNotType(opplist, "Creature").isEmpty() && CardLists.getNotType(ailist, "Creature").isEmpty()) {
            if (AiTeams.outweighs(ComputerUtilCard.evaluateCreatureList(opplist), ComputerUtilCard.evaluateCreatureList(ailist),
                    sides, CREATURE_EVAL_THRESHOLD)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
            }

            // test whether any opponent can kill the ai next turn
            for (Player opponent : opponents) {
                Combat combat = new Combat(opponent);
                boolean containsAttacker = false;
                for (Card att : opponent.getCreaturesInPlay()) {
                    if (ComputerUtilCombat.canAttackNextTurn(att, ai)) {
                        combat.addAttacker(att, ai);
                        containsAttacker = containsAttacker || opplist.contains(att);
                    }
                }
                if (!containsAttacker) {
                    continue;
                }
                AiBlockController block = new AiBlockController(ai, false);
                block.assignBlockersForCombat(combat);

                if (ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } // only lands involved
        else if (CardLists.getNotType(opplist, "Land").isEmpty() && CardLists.getNotType(ailist, "Land").isEmpty()) {
            if (side.anyMatch(p -> p.isCardInPlay("Crucible of Worlds")) && !opponents.anyMatch(p -> p.isCardInPlay("Crucible of Worlds"))) {
                // TODO Should care about any land recursion, not just Crucible of Worlds
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            // evaluate the situation with creatures on the battlefield separately, as that's where the AI typically makes mistakes
            CardCollection aiCreatures = side.getCreaturesInPlay();
            CardCollection oppCreatures = opponents.getCreaturesInPlay();
            // land destruction pays off for whoever is ahead on board
            if (!oppCreatures.isEmpty() && ComputerUtilCard.evaluateCreatureList(aiCreatures) * sides
                    < ComputerUtilCard.evaluateCreatureList(oppCreatures) + CREATURE_EVAL_THRESHOLD) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            // check if the AI would lose more lands than the opponents would
            if (ComputerUtilCard.evaluatePermanentList(ailist) * sides > ComputerUtilCard.evaluatePermanentList(opplist) + sides) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
        else if (!AiTeams.outweighs(ComputerUtilCard.evaluatePermanentList(opplist), ComputerUtilCard.evaluatePermanentList(ailist), sides, 3)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card source = sa.getHostCard();
        if (payers.size() > 1) {
            if (alreadyPaid) {
                return false;
            }
        }
        String valid = sa.getParamOrDefault("ValidCards", "");

        CardCollection ailist = CardLists.getValidCards(payer.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);
        ailist = CardLists.filter(ailist, predicate);

        if (ailist.isEmpty()) {
            return false;
        }

        if (cost.hasSpecificCostType(CostDamage.class)) {
            if (!payer.canLoseLife()) {
                return false;
            }
            final CostDamage pay = cost.getCostPartByType(CostDamage.class);
            int realDamage = ComputerUtilCombat.predictDamageTo(payer, pay.getAbilityAmount(sa), source, false);
            if (realDamage > payer.getLife()) {
                return false;
            }
            if (realDamage > ailist.size() * 3) { // three life points per one creature
                return false;
            }
        }

        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}