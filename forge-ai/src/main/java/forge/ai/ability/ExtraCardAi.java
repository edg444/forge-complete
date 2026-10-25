package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The last one-card logics of the no-AI pass.
 * <ul>
 * <li>ChemistExile / ChemistCast (Izzet Chemister): its graveyard's instants and sorceries banked at their end step,
 * then cast for free once two or more are.</li>
 * <li>Feather (Aven Mimeomancer, in its upkeep): their best creature made a 3/1 when it's worth much more.</li>
 * <li>PackHunt (Pack Hunt): its creature with the most copies left in its library.</li>
 * <li>AlphaBrawl (Alpha Brawl): their creature whose fight with the rest of their board kills the most.</li>
 * <li>ToughFight (Grim Contest): its sturdiest creature against their best creature that toughness kills.</li>
 * <li>Deploy (Surprise Deployment): a creature from hand as a blocker that kills an attacker, or when their attack
 * would kill it.</li>
 * <li>FlashbackSetup (Quiet Speculation): flashback cards from its own library, two or more.</li>
 * <li>Chariot (Chariot of the Sun): flying for its attacker of 3 or more power when they have nothing to block it.</li>
 * </ul>
 */
public final class ExtraCardAi {

    private ExtraCardAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "ChemistExile", "ChemistCast", "Feather", "PackHunt", "AlphaBrawl", "ToughFight", "Deploy", "FlashbackSetup",
                    "Chariot" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "ChemistExile" -> chemistExile(ai, sa);
            case "ChemistCast" -> chemistCast(ai, sa);
            case "Feather" -> feather(ai, sa);
            case "PackHunt" -> packHunt(ai, sa);
            case "AlphaBrawl" -> alphaBrawl(ai, sa);
            case "ToughFight" -> toughFight(ai, sa);
            case "Deploy" -> deploy(ai, sa);
            case "FlashbackSetup" -> flashbackSetup(ai, sa);
            default -> chariot(ai, sa);
        };
    }

    private static boolean theirEnd(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static boolean ownMain(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)) && ai.getGame().getStack().isEmpty();
    }

    private static AiAbilityDecision chemistExile(final Player ai, final SpellAbility sa) {
        if (!theirEnd(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Card best = CardUtil.getValidCardsToTarget(sa).stream().filter(c -> c.getOwner().equals(ai))
                .max(java.util.Comparator.comparingInt(Card::getCMC)).orElse(null);
        if (best == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision chemistCast(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final int banked = (int) ai.getGame().getCardsIn(ZoneType.Exile).stream().filter(c -> host.equals(c.getExiledWith())).count();
        return banked >= 2 && ownMain(ai) ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision feather(final Player ai, final SpellAbility sa) {
        // a 3/1 flier rates about 170
        final Card best = CardUtil.getValidCardsToTarget(sa).stream().filter(c -> c.getController().isOpponentOf(ai))
                .max(java.util.Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 240) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision packHunt(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        Card best = null;
        int most = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            final int n = CardLists.count(ai.getCardsIn(ZoneType.Library), l -> l.getName().equals(c.getName()));
            if (n > most) {
                most = n;
                best = c;
            }
        }
        if (best == null || most < 2) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision alphaBrawl(final Player ai, final SpellAbility sa) {
        Card best = null;
        int bestV = 0;
        for (final Card t : CardUtil.getValidCardsToTarget(sa)) {
            final List<Card> rest = CardLists.filter(t.getController().getCreaturesInPlay(), c -> !c.equals(t));
            int v = 0;
            int back = 0;
            for (final Card c : rest) {
                if (t.getNetPower() >= c.getLethalDamage() && c.canBeDestroyed()) {
                    v += ComputerUtilCard.evaluateCreature(c);
                }
                back += c.getNetPower();
            }
            if (back >= t.getLethalDamage() && t.canBeDestroyed()) {
                v += ComputerUtilCard.evaluateCreature(t);
            }
            if (v > bestV) {
                bestV = v;
                best = t;
            }
        }
        if (best == null || bestV < 250) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision toughFight(final Player ai, final SpellAbility sa) {
        final SpellAbility other = sa.getSubAbility();
        if (other == null || !other.usesTargeting()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        int bestV = 0;
        Card mineBest = null;
        Card theirsBest = null;
        for (final Card mine : CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai))) {
            for (final Card theirs : CardUtil.getValidCardsToTarget(other)) {
                if (!theirs.getController().isOpponentOf(ai) || mine.getNetToughness() < theirs.getLethalDamage()) {
                    continue;
                }
                int v = ComputerUtilCard.evaluateCreature(theirs);
                if (theirs.getNetToughness() >= mine.getLethalDamage()) {
                    v -= ComputerUtilCard.evaluateCreature(mine);
                }
                if (v > bestV) {
                    bestV = v;
                    mineBest = mine;
                    theirsBest = theirs;
                }
            }
        }
        if (mineBest == null || bestV < 100) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(mineBest);
        other.resetTargets();
        other.getTargets().add(theirsBest);
        return YES;
    }

    private static AiAbilityDecision deploy(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final boolean danger = ComputerUtilCombat.lifeInDanger(ai, combat);
        Card best = null;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.isCreature() || c.getColor().hasWhite()) {
                continue;
            }
            final boolean kills = combat.getAttackersOf(ai).stream().anyMatch(a -> CombatUtil.canBlock(a, c)
                    && c.getNetCombatDamage() >= a.getLethalDamage() && a.getNetCombatDamage() < c.getLethalDamage());
            if ((kills || danger) && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
            }
        }
        if (best == null) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        ZonePickAi.plan(sa.getHostCard(), List.of(best));
        return YES;
    }

    private static AiAbilityDecision flashbackSetup(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai) || !sa.canTarget(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int n = CardLists.count(ai.getCardsIn(ZoneType.Library), c -> c.hasKeyword(forge.game.keyword.Keyword.FLASHBACK));
        if (n < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(ai);
        return YES;
    }

    private static AiAbilityDecision chariot(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final boolean flyers = ai.getOpponents().getCreaturesInPlay().stream().anyMatch(c -> c.isUntapped()
                && (c.hasKeyword(forge.game.keyword.Keyword.FLYING) || c.hasKeyword(forge.game.keyword.Keyword.REACH)));
        if (flyers) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> combat.isAttacking(c) && c.getNetPower() >= 3
                && !c.hasKeyword(forge.game.keyword.Keyword.FLYING)).stream()
                .max(java.util.Comparator.comparingInt(Card::getNetPower)).orElse(null);
        if (best == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }
}
