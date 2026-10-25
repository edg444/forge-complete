package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Hand, library and one-combat logics for the last of the no-AI cards.
 * <ul>
 * <li>VenomFight (Venomous Breath): once blocks are known, the fight where destroying the other side helps most.</li>
 * <li>Dragons (Descent of the Dragons): its creatures worth less than a 4/4 flying Dragon, and theirs worth much more.</li>
 * <li>RecallX (Recall): spare cards traded for its best graveyard cards, two or more.</li>
 * <li>GrimoireStudy / GrimoireRaise (Grimoire of the Dead): a spare card at their end step, then every graveyard's
 * creatures once it's worth it.</li>
 * <li>DiscardRed / WheelThree (Chandra Ablaze): a red card discarded for the 4 damage; everyone's hand swapped for
 * three when its own hand is nearly empty and theirs isn't.</li>
 * <li>RingSwap (Jandor's Ring): a land drawn this turn it doesn't need.</li>
 * <li>DividedX (Meteor Shower): X+1 damage, their creatures it kills first and the rest to their face.</li>
 * <li>TwoKill (Ruthless Disposal): their two best creatures, for a creature it can spare.</li>
 * <li>Filter (Read the Runes, Laquatus's Creativity): cards drawn and discarded at their end step, or in its second
 * main phase for a sorcery.</li>
 * <li>UpkeepDraw (Aladdin's Lamp, Mangara's Tome): in its upkeep, before the draw it changes.</li>
 * </ul>
 */
public final class HandAi {

    private HandAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "VenomFight", "Dragons", "RecallX", "GrimoireStudy", "GrimoireRaise", "DiscardRed", "WheelThree", "RingSwap",
                    "DividedX", "TwoKill", "Filter", "UpkeepDraw" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "VenomFight" -> venom(ai, sa);
            case "Dragons" -> dragons(ai, sa);
            case "RecallX" -> recall(ai, sa);
            case "GrimoireStudy" -> theirEnd(ai) && spare(ai, sa.getHostCard()) > 0 ? YES : no(AiPlayDecision.AnotherTime);
            case "GrimoireRaise" -> grimoireRaise(ai, sa);
            case "DiscardRed" -> discardRed(ai, sa);
            case "WheelThree" -> wheel(ai, sa);
            case "RingSwap" -> ringSwap(ai, sa);
            case "DividedX" -> dividedX(ai, sa);
            case "TwoKill" -> twoKill(ai, sa);
            case "Filter" -> filter(ai, sa);
            default -> upkeepDraw(ai, sa);
        };
    }

    private static boolean theirEnd(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static boolean ownMain2(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2);
    }

    private static int spare(final Player ai, final Card host) {
        final CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        hand.remove(host);
        int n = 0;
        for (Card c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand); c != null;
                c = ComputerUtil.getCardPreference(ai, host, "DiscardCost", hand)) {
            hand.remove(c);
            n++;
        }
        return n;
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
    }

    private static AiAbilityDecision venom(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        Card best = null;
        int bestV = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!combat.isAttacking(c) && !combat.isBlocking(c)) {
                continue;
            }
            int v = 0;
            for (final Card o : combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c)) {
                if (o.canBeDestroyed()) {
                    v += (o.getController().isOpponentOf(ai) ? 1 : -1) * ComputerUtilCard.evaluateCreature(o);
                }
            }
            if (v > bestV) {
                bestV = v;
                best = c;
            }
        }
        if (best == null || bestV < 120) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision dragons(final Player ai, final SpellAbility sa) {
        if (!ownMain2(ai) && !(ai.getGame().getPhaseHandler().isPlayerTurn(ai) && ai.getGame().getPhaseHandler().is(PhaseType.MAIN1))) {
            return no(AiPlayDecision.AnotherTime);
        }
        // a 4/4 flying Dragon token rates about this much
        final int dragon = 250;
        final List<Card> picks = new ArrayList<>();
        int gain = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            final int v = ComputerUtilCard.evaluateCreature(c);
            if (!c.canBeDestroyed()) {
                continue;
            }
            if (c.getController().equals(ai) && v + 60 < dragon) {
                picks.add(c);
                gain += dragon - v;
            } else if (c.getController().isOpponentOf(ai) && v > dragon + 80) {
                picks.add(c);
                gain += v - dragon;
            }
        }
        if (gain < 150) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        picks.forEach(c -> sa.getTargets().add(c));
        return YES;
    }

    private static AiAbilityDecision recall(final Player ai, final SpellAbility sa) {
        if (!ownMain2(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int good = CardLists.count(ai.getCardsIn(ZoneType.Graveyard), c -> !c.isLand() && c.getCMC() >= 2);
        final int x = Math.min(Math.min(spare(ai, sa.getHostCard()), good), xMana(ai, sa));
        if (x < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    private static int xMana(final Player ai, final SpellAbility sa) {
        final forge.card.mana.ManaCost cost = sa.getPayCosts() == null ? null : sa.getPayCosts().getTotalMana();
        if (cost == null) {
            return ComputerUtilMana.getAvailableManaEstimate(ai, false);
        }
        return Math.max(0, (ComputerUtilMana.getAvailableManaEstimate(ai, false) - cost.getCMC()) / Math.max(1, cost.countX()));
    }

    private static AiAbilityDecision grimoireRaise(final Player ai, final SpellAbility sa) {
        int v = 0;
        for (final Player p : ai.getGame().getPlayers()) {
            for (final Card c : p.getCardsIn(ZoneType.Graveyard)) {
                if (c.isCreature()) {
                    v += ComputerUtilCard.evaluateCreature(c);
                }
            }
        }
        return v >= 300 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision discardRed(final Player ai, final SpellAbility sa) {
        final boolean red = ai.getCardsIn(ZoneType.Hand).anyMatch(c -> c.getColor().hasRed());
        return red ? YES : no(AiPlayDecision.CantPlayAi);
    }

    /** Chandra Ablaze's +1: its least valuable red card, for the 4 damage. */
    public static Card redDiscard(final SpellAbility sa, final CardCollection valid) {
        if (!"DiscardRed".equals(sa.getRootAbility().getParam("AILogic"))) {
            return null;
        }
        return valid.stream().filter(c -> c.getColor().hasRed()).min(Comparator.comparingInt(HandAi::worth)).orElse(null);
    }

    private static AiAbilityDecision wheel(final Player ai, final SpellAbility sa) {
        final Player opp = ai.getStrongestOpponent();
        final int mine = ai.getCardsIn(ZoneType.Hand).size();
        return opp != null && mine <= 1 && opp.getCardsIn(ZoneType.Hand).size() >= 2 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision ringSwap(final Player ai, final SpellAbility sa) {
        final Card last = ai.getLastDrawnCard();
        final boolean flooded = ai.getLandsInPlay().size() >= 5;
        return last != null && last.isLand() && flooded && ai.getCardsIn(ZoneType.Hand).contains(last) ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision dividedX(final Player ai, final SpellAbility sa) {
        final int x = xMana(ai, sa);
        int dmg = x + 1;
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || dmg < 3) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.setXManaCostPaid(x);
        sa.resetTargets();
        final List<Card> kills = new ArrayList<>(CardLists.filter(opp.getCreaturesInPlay(), c -> sa.canTarget(c) && c.canBeDestroyed()));
        kills.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        int value = 0;
        final boolean lethal = opp.canLoseLife() && dmg >= opp.getLife() && sa.canTarget(opp);
        if (!lethal) {
            for (final Card c : kills) {
                final int need = c.getLethalDamage();
                if (need <= dmg) {
                    sa.getTargets().add(c);
                    sa.addDividedAllocation(c, need);
                    dmg -= need;
                    value += ComputerUtilCard.evaluateCreature(c);
                }
            }
        }
        if (dmg > 0 && sa.canTarget(opp)) {
            sa.getTargets().add(opp);
            sa.addDividedAllocation(opp, dmg);
            value += lethal ? 1000 : dmg * 15;
        }
        if (value < 150) {
            sa.resetTargets();
            sa.setXManaCostPaid(null);
            return no(AiPlayDecision.CantPlayAi);
        }
        return YES;
    }

    private static AiAbilityDecision twoKill(final Player ai, final SpellAbility sa) {
        final List<Card> theirs = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().isOpponentOf(ai)));
        theirs.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        final Card give = ComputerUtilCard.getWorstCreatureAI(ai.getCreaturesInPlay());
        if (theirs.size() < 2 || give == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        final int gain = ComputerUtilCard.evaluateCreature(theirs.get(0)) + ComputerUtilCard.evaluateCreature(theirs.get(1))
                - ComputerUtilCard.evaluateCreature(give) - 80;
        if (gain < 200) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(theirs.get(0));
        sa.getTargets().add(theirs.get(1));
        SacrificePayoffAi.plan(sa.getHostCard(), give);
        return YES;
    }

    private static AiAbilityDecision filter(final Player ai, final SpellAbility sa) {
        final boolean sorcery = sa.getHostCard().isSorcery();
        if (sorcery ? !ownMain2(ai) : !theirEnd(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        if (sa.usesTargeting()) {
            // Laquatus's Creativity: its own hand, three or more cards to churn
            if (!sa.canTarget(ai) || ai.getCardsIn(ZoneType.Hand).size() < 4) {
                return no(AiPlayDecision.CantPlayAi);
            }
            sa.resetTargets();
            sa.getTargets().add(ai);
            return YES;
        }
        final int x = xMana(ai, sa);
        if (x < 2) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    private static AiAbilityDecision upkeepDraw(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.UPKEEP)) {
            return no(AiPlayDecision.AnotherTime);
        }
        if ("Count$xPaid".equals(sa.getSVar("X"))) {
            sa.setXManaCostPaid(null);
            final int x = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
            if (x < 2) {
                return no(AiPlayDecision.CantAfford);
            }
            sa.setXManaCostPaid(x);
            return YES;
        }
        // Mangara's Tome: only while its pile lasts
        return sa.getHostCard().getRemembered().iterator().hasNext() ? YES : no(AiPlayDecision.CantPlayAi);
    }
}
