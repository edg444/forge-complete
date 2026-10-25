package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
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

/**
 * Small one-card combat logics.
 * <ul>
 * <li>AILogic$ Dagger (Acidic Dagger): on its own attacker, as it attacks into creatures that can block it.</li>
 * <li>AILogic$ TapAllBefore (Aether Shockwave's modes): on their turn before attacks, when what the mode taps of theirs
 * outweighs what it taps of its own.</li>
 * <li>AILogic$ AnimateX (Chimeric Staff, Chimeric Coils): X as large as the mana allows, on their attack as a blocker
 * that kills an attacker and lives (or when the attack would kill it), and on its own turn when the attack is lethal
 * with nothing of theirs able to block it.</li>
 * <li>AILogic$ Damnation (Choice of Damnations): cast when they have clearly more permanents, or little life.</li>
 * </ul>
 */
public final class MiscCombatAi {

    private MiscCombatAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    public static boolean handles(final String logic) {
        return "Dagger".equals(logic) || "TapAllBefore".equals(logic) || "AnimateX".equals(logic) || "Damnation".equals(logic);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "Dagger" -> dagger(ai, sa);
            case "TapAllBefore" -> tapAllBefore(ai, sa);
            case "AnimateX" -> animateX(ai, sa);
            default -> damnation(ai, sa);
        };
    }

    private static AiAbilityDecision dagger(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        Card best = null;
        for (final Card a : CardUtil.getValidCardsToTarget(sa)) {
            if (!combat.isAttacking(a) || a.getNetCombatDamage() <= 0) {
                continue;
            }
            final Player def = combat.getDefenderPlayerByAttacker(a);
            final boolean blockable = def != null && def.getCreaturesInPlay().anyMatch(b -> !b.getType().hasCreatureType("Wall")
                    && CombatUtil.canBlock(a, b));
            if (blockable && (best == null || a.getNetCombatDamage() > best.getNetCombatDamage())) {
                best = a;
            }
        }
        if (best == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision tapAllBefore(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.isPlayerTurn(ai) || !ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final String valid = sa.getParamOrDefault("ValidCards", "Creature");
        int theirs = 0;
        int mine = 0;
        for (final Card c : CardLists.getValidCards(ai.getGame().getCardsIn(forge.game.zone.ZoneType.Battlefield), valid.split(","), ai, sa.getHostCard(), sa)) {
            if (!c.isCreature() || c.isTapped()) {
                continue;
            }
            if (c.getController().isOpponentOf(ai)) {
                theirs += c.getNetCombatDamage();
            } else {
                mine += c.getNetCombatDamage();
            }
        }
        return theirs >= 4 && theirs > mine ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision animateX(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Card host = sa.getHostCard();
        if (host.isCreature()) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.setXManaCostPaid(null);
        final int x = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        if (x < 2) {
            return no(AiPlayDecision.CantAfford);
        }
        final PhaseHandler ph = game.getPhaseHandler();
        final Combat combat = game.getCombat();
        if (!ph.isPlayerTurn(ai)) {
            // a blocker, once they've attacked
            if (combat == null || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return no(AiPlayDecision.WaitForCombat);
            }
            final boolean kills = combat.getAttackersOf(ai).anyMatch(a -> a.getNetToughness() <= x && a.getNetCombatDamage() < x
                    && !a.hasKeyword(forge.game.keyword.Keyword.FLYING) && ComputerUtilCard.evaluateCreature(a) >= 120);
            if (!kills && !ComputerUtilCombat.lifeInDanger(ai, combat)) {
                return no(AiPlayDecision.DoesntImpactCombat);
            }
            sa.setXManaCostPaid(x);
            return YES;
        }
        if (!ph.is(PhaseType.MAIN1)) {
            return no(AiPlayDecision.AnotherTime);
        }
        for (final Player opp : ai.getOpponents()) {
            if (opp.getCreaturesInPlay().anyMatch(Card::isUntapped) || !opp.canLoseLife()) {
                continue;
            }
            int dmg = x;
            for (final Card c : ai.getCreaturesInPlay()) {
                if (CombatUtil.canAttack(c)) {
                    dmg += c.getNetCombatDamage();
                }
            }
            if (dmg >= opp.getLife()) {
                sa.setXManaCostPaid(x);
                return YES;
            }
        }
        return no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision damnation(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !sa.canTarget(opp)) {
            return no(AiPlayDecision.TargetingFailed);
        }
        final int theirs = opp.getCardsIn(forge.game.zone.ZoneType.Battlefield).size();
        final int mine = ai.getCardsIn(forge.game.zone.ZoneType.Battlefield).size();
        if (theirs - mine < 3 && opp.getLife() > 5) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }

    /** Choice of Damnations, as it resolves: their permanents when the number leaves several to sacrifice, else life. */
    public static SpellAbility damnationChoice(final SpellAbility sa, final java.util.List<SpellAbility> spells) {
        final Player opp = sa.getRootAbility().getTargets().getFirstTargetedPlayer();
        final Integer n = sa.getHostCard().getChosenNumber();
        if (opp == null || n == null) {
            return spells.get(0);
        }
        final int lost = opp.getCardsIn(forge.game.zone.ZoneType.Battlefield).size() - n;
        final boolean sacrifice = n < opp.getLife() && (lost >= 2 || n < 3);
        for (final SpellAbility s : spells) {
            if ((s.getApi() == forge.game.ability.ApiType.Sacrifice) == sacrifice) {
                return s;
            }
        }
        return spells.get(0);
    }
}
