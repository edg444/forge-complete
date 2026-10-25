package forge.ai.ability;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The fork's Un-cards that the AI can play straight.
 * <ul>
 * <li>EvilPresents (Evil Presents): its biggest creature in hand given to them, attacking them each turn.</li>
 * <li>MoonmistFog (That's No Moonmist): their non-Phyrexian combat damage prevented when their attack would kill
 * it.</li>
 * <li>ChaosWrap (Chaos Wrap): their permanent worth more than the 4/4 their top card might become.</li>
 * <li>The Vegas nights' upkeep modes (see {@link #chooseNight}).</li>
 * </ul>
 */
public final class UnCardAi {

    private UnCardAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return "EvilPresents".equals(logic) || "MoonmistFog".equals(logic) || "ChaosWrap".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "EvilPresents" -> evilPresents(ai, sa);
            case "ChaosWrap" -> chaosWrap(ai, sa);
            default -> moonmist(ai, sa);
        };
    }

    private static AiAbilityDecision evilPresents(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        // it attacks its new controller each turn, so the more power the better
        final Card gift = CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isCreature).stream()
                .max(Comparator.comparingInt(Card::getNetPower)).orElse(null);
        if (opp == null || !sa.canTarget(opp) || gift == null || gift.getNetPower() < 3) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        ZonePickAi.plan(sa.getHostCard(), List.of(gift));
        return YES;
    }

    private static AiAbilityDecision moonmist(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        return ComputerUtilCombat.lifeInDanger(ai, combat) ? YES : no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision chaosWrap(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Combat combat = ai.getGame().getCombat();
        final boolean theirAttack = combat != null && !combat.getAttackingPlayer().equals(ai) && ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS);
        if (!theirAttack && !(ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2)) && !(!ph.isPlayerTurn(ai) && ph.is(PhaseType.END_OF_TURN))) {
            return no(AiPlayDecision.AnotherTime);
        }
        // its owner then may put their top card onto the battlefield, a 4/4 if nothing better
        final List<Card> worth = CardLists.filter(ai.getOpponents().getCardsIn(ZoneType.Battlefield), c -> sa.canTarget(c)
                && (!theirAttack || combat.isAttacking(c))
                && (c.isCreature() ? ComputerUtilCard.evaluateCreature(c) > 200 : c.isPlaneswalker() || !c.isLand() && c.getCMC() >= 4));
        final Card best = ComputerUtilCard.getBestAI(worth);
        if (best == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    public static boolean handlesCharm(final SpellAbility sa) {
        final String name = sa.getHostCard().getName();
        return "Another Night in Vegas".equals(name) || "Yet Another Night in Vegas".equals(name);
    }

    /**
     * Another Night in Vegas, Yet Another Night in Vegas: one mode each upkeep, none twice, so the best one now, leaving
     * the modes that end the night for last. Its own creature for the modes that target, never theirs while it can wait.
     */
    public static List<AbilitySub> chooseNight(final Player ai, final List<AbilitySub> choices) {
        AbilitySub best = null;
        Card bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        for (final AbilitySub sub : choices) {
            sub.setActivatingPlayer(ai);
            Card target = null;
            int score;
            if (sub.usesTargeting()) {
                final boolean graveyard = sub.getApi() == ApiType.ChangeZone;
                final List<Card> mine = CardLists.filter(ai.getCardsIn(graveyard ? ZoneType.Graveyard : ZoneType.Battlefield),
                        sub::canTarget);
                final List<Card> theirs = CardLists.filter(ai.getOpponents().getCardsIn(graveyard ? ZoneType.Graveyard : ZoneType.Battlefield),
                        sub::canTarget);
                if (!mine.isEmpty()) {
                    target = ComputerUtilCard.getBestCreatureAI(mine);
                    score = graveyard ? 90 : 60;
                } else if (!theirs.isEmpty()) {
                    target = ComputerUtilCard.getWorstCreatureAI(theirs);
                    score = -10;
                } else {
                    continue;
                }
            } else {
                score = switch (sub.getApi()) {
                    case Surveil -> 80;
                    case ChangeZone -> 70;
                    case Effect -> 50;
                    // Get lost: each exiles a creature, best when it has none to lose
                    case RepeatEach -> nightExile(ai);
                    // go again: all modes back for 5 life
                    case Cleanup -> ai.getLife() > 10 ? 40 : -100;
                    default -> 0;
                };
            }
            if (score > bestScore) {
                best = sub;
                bestTarget = target;
                bestScore = score;
            }
        }
        if (best == null) {
            return Lists.newArrayList();
        }
        if (bestTarget != null) {
            best.resetTargets();
            best.getTargets().add(bestTarget);
        }
        return Lists.newArrayList(best);
    }

    private static int nightExile(final Player ai) {
        final int mine = ai.getCreaturesInPlay().size();
        final int theirs = ai.getOpponents().getCreaturesInPlay().size();
        if (theirs == 0) {
            return -20;
        }
        return mine == 0 ? 75 : mine <= theirs ? 10 : -20;
    }
}
