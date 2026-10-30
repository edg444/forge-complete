package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiTeams;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The final one-card logics of the no-AI pass.
 * <ul>
 * <li>Dazzle (Dazzling Reflection): their biggest attacker's damage prevented, its power gained as life.</li>
 * <li>PingTeam (Lightning Volley, Flame Fusillade): at their end step, with enough untapped creatures or permanents
 * to ping with.</li>
 * <li>PastInFlames (Past in Flames): in its first main phase, with mana for two or more of its graveyard's spells.</li>
 * <li>PersistSave (Cauldron of Souls): persist for its creatures about to die.</li>
 * <li>KeepPower4 / KeepTwo (Destined Confrontation, Covetous Elegy): symmetric wipes, when they lose clearly more.</li>
 * <li>Simulacrum (Simulacrum): the damage it took this turn back as life, dealt to a creature of its own that
 * survives it or matters least.</li>
 * <li>LockPermanent / LockTapped (Immovable Rod, Giant Oyster): their best creature locked down, kept tapped with
 * AIUntapPreference LockEffect.</li>
 * <li>TeamPumpLethal (Dark Triumph): once blocks are known, +2/+0 that makes the unblocked attackers lethal.</li>
 * <li>BalthorRaise (Balthor the Defiled): when its graveyard's black and red creatures outweigh theirs.</li>
 * </ul>
 */
public final class LastCardAi {

    private LastCardAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    /** Destined Confrontation: its NegativePowerFirst is each player's choice as it resolves; casting it is this. */
    public static boolean handlesSa(final SpellAbility sa) {
        return "NegativePowerFirst".equals(sa.getParam("AILogic")) && sa.hasParam("WithTotalPower");
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "Dazzle", "PingTeam", "PastInFlames", "PersistSave", "KeepPower4", "KeepTwo", "Simulacrum", "LockPermanent",
                    "LockTapped", "TeamPumpLethal", "BalthorRaise" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "NegativePowerFirst" -> keep(ai, sa, true);
            case "Dazzle" -> dazzle(ai, sa);
            case "PingTeam" -> pingTeam(ai, sa);
            case "PastInFlames" -> pastInFlames(ai, sa);
            case "PersistSave" -> persistSave(ai, sa);
            case "KeepPower4" -> keep(ai, sa, true);
            case "KeepTwo" -> keep(ai, sa, false);
            case "Simulacrum" -> simulacrum(ai, sa);
            case "LockPermanent" -> lock(ai, sa, false);
            case "LockTapped" -> lock(ai, sa, true);
            case "TeamPumpLethal" -> teamPump(ai, sa);
            default -> balthor(ai, sa);
        };
    }

    private static boolean theirEnd(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static AiAbilityDecision dazzle(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        final Card best = combat.getAttackersOf(ai).stream().filter(sa::canTarget).max(Comparator.comparingInt(Card::getNetCombatDamage)).orElse(null);
        if (best == null || best.getNetCombatDamage() < 3) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision pingTeam(final Player ai, final SpellAbility sa) {
        if (!theirEnd(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final boolean creatures = sa.getParamOrDefault("ValidCards", "").startsWith("Creature");
        final int n = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isUntapped() && (!creatures || c.isCreature() && !c.isSick()));
        return n >= (creatures ? 2 : 4) ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision pastInFlames(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return no(AiPlayDecision.AnotherTime);
        }
        int mana = ComputerUtilMana.getAvailableManaEstimate(ai, false) - sa.getPayCosts().getTotalMana().getCMC();
        final List<Card> spells = new ArrayList<>(CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), c -> c.isInstant() || c.isSorcery()));
        spells.sort(Comparator.comparingInt(Card::getCMC));
        int castable = 0;
        for (final Card c : spells) {
            if (c.getCMC() <= mana) {
                mana -= c.getCMC();
                castable++;
            }
        }
        return castable >= 2 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision persistSave(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final List<GameObject> doomed = game.getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
        sa.resetTargets();
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().equals(ai) || c.getCounters(forge.game.card.CounterEnumType.M1M1) > 0) {
                continue;
            }
            final boolean dies = doomed.contains(c) || combat != null && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && (combat.isAttacking(c) || combat.isBlocking(c)) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat);
            if (dies) {
                sa.getTargets().add(c);
            }
        }
        return sa.getTargets().isEmpty() ? no(AiPlayDecision.AnotherTime) : YES;
    }

    // what a player loses keeping the best of its creatures within the limit
    private static int loss(final Player p, final boolean byPower) {
        final List<Card> mine = new ArrayList<>(p.getCreaturesInPlay());
        mine.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        int lost = 0;
        int power = 0;
        int kept = 0;
        for (final Card c : mine) {
            final boolean keep = byPower ? power + Math.max(0, c.getNetPower()) <= 4 : kept < 2;
            if (keep) {
                power += Math.max(0, c.getNetPower());
                kept++;
            } else {
                lost += ComputerUtilCard.evaluateCreature(c);
            }
        }
        return lost;
    }

    private static AiAbilityDecision keep(final Player ai, final SpellAbility sa, final boolean byPower) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        int theirs = 0;
        for (final Player opp : ai.getOpponents()) {
            theirs += loss(opp, byPower);
        }
        return theirs - loss(ai, byPower) >= 200 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision simulacrum(final Player ai, final SpellAbility sa) {
        final int dmg = forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(), "X", sa);
        if (dmg < 4) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final SpellAbility hit = sa.getSubAbility();
        if (hit == null || !hit.usesTargeting()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final List<Card> mine = CardLists.filter(CardUtil.getValidCardsToTarget(hit), c -> c.getController().equals(ai));
        Card pick = mine.stream().filter(c -> c.getLethalDamage() > dmg).min(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (pick == null && dmg >= 6) {
            pick = ComputerUtilCard.getWorstCreatureAI(mine);
        }
        if (pick == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        hit.resetTargets();
        hit.getTargets().add(pick);
        return YES;
    }

    private static AiAbilityDecision lock(final Player ai, final SpellAbility sa, final boolean tappedOnly) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        // Giant Oyster: their attackers stay tapped from their turn; Immovable Rod before an attack
        if (tappedOnly ? !theirEnd(ai) : !ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Card best = CardUtil.getValidCardsToTarget(sa).stream()
                .filter(c -> c.getController().isOpponentOf(ai) && c.isCreature() && (!tappedOnly || c.isTapped()))
                .max(Comparator.comparingInt(ComputerUtilCard::evaluateCreature)).orElse(null);
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 160) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision teamPump(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        for (final Player opp : ai.getOpponents()) {
            final int open = CardLists.count(combat.getUnblockedAttackers(), a -> opp.equals(combat.getDefenderPlayerByAttacker(a)));
            final int need = ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
            if (open > 0 && need > 0 && open * 2 >= need && opp.canLoseLife()) {
                // the alternative cost: a creature that isn't in the fight
                final Card give = ComputerUtilCard.getWorstCreatureAI(CardLists.filter(ai.getCreaturesInPlay(),
                        c -> !combat.isAttacking(c) && !combat.isBlocking(c)));
                if (give != null) {
                    SacrificePayoffAi.plan(sa.getHostCard(), give);
                }
                return YES;
            }
        }
        return no(AiPlayDecision.DoesntImpactCombat);
    }

    private static AiAbilityDecision balthor(final Player ai, final SpellAbility sa) {
        int v = 0;
        for (final Player p : ai.getGame().getPlayers()) {
            for (final Card c : p.getCardsIn(ZoneType.Graveyard)) {
                if (c.isCreature() && (c.getColor().hasBlack() || c.getColor().hasRed())) {
                    v -= AiTeams.sign(ai, p) * ComputerUtilCard.evaluateCreature(c);
                }
            }
        }
        return v >= 250 ? YES : no(AiPlayDecision.CantPlayAi);
    }
}
