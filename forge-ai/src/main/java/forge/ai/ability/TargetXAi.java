package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * AILogic$ TargetX: X depends on what's targeted, so each target is tried with its own X.
 * <ul>
 * <li>Damage (Weight of Spires, Whipkeeper): their best creature its X kills.</li>
 * <li>Pump (Great Defender, Might of the Nephilim, Surge of Strength, Nantuko Mentor): once blocks are known, its
 * creature the pump saves, lets kill what it fights, or makes lethal unblocked.</li>
 * <li>A spell on the stack for damage to its controller (Parallectric Feedback, Refuse): their priciest spell, when
 * it's 3 or more or lethal.</li>
 * </ul>
 */
public final class TargetXAi {

    private TargetXAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static int amount(final SpellAbility sa, final String param) {
        final String v = sa.getParamOrDefault(param, "0").replace("+", "");
        return AbilityUtils.calculateAmount(sa.getHostCard(), v, sa);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        if (sa.getApi() == ApiType.DealDamage) {
            return kill(ai, sa);
        }
        if (sa.getApi() == ApiType.Pump && "Stack".equals(sa.getParam("TgtZone"))) {
            return spellBurn(ai, sa);
        }
        if (sa.getApi() == ApiType.Pump) {
            return combatPump(ai, sa);
        }
        return no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision kill(final Player ai, final SpellAbility sa) {
        Card best = null;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().isOpponentOf(ai) || !c.canBeDestroyed()) {
                continue;
            }
            sa.resetTargets();
            sa.getTargets().add(c);
            if (amount(sa, "NumDmg") >= c.getLethalDamage()
                    && (best == null || ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(best))) {
                best = c;
            }
        }
        sa.resetTargets();
        if (best == null || ComputerUtilCard.evaluateCreature(best) < 100) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision spellBurn(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        SpellAbility best = null;
        int bestMv = 0;
        for (final SpellAbilityStackInstance si : game.getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s.isSpell() && s.getActivatingPlayer().isOpponentOf(ai) && sa.canTargetSpellAbility(s)
                    && s.getHostCard().getCMC() > bestMv) {
                best = s;
                bestMv = s.getHostCard().getCMC();
            }
        }
        if (best == null) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player them = best.getActivatingPlayer();
        if (bestMv < 3 && bestMv < them.getLife()) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // what fighting does to a creature: the damage it takes and the creatures it's fighting
    private static int damageTo(final Card c, final Combat combat) {
        int dmg = 0;
        for (final Card o : combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c)) {
            dmg += o.getNetCombatDamage();
        }
        return dmg;
    }

    private static AiAbilityDecision combatPump(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        Card best = null;
        int bestValue = 0;
        for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
            if (!c.getController().equals(ai) || !combat.isAttacking(c) && !combat.isBlocking(c)) {
                continue;
            }
            sa.resetTargets();
            sa.getTargets().add(c);
            final int att = amount(sa, "NumAtt");
            final int def = amount(sa, "NumDef");
            final boolean trample = sa.getParamOrDefault("KW", "").contains("Trample") || c.hasKeyword(Keyword.TRAMPLE);
            final int left = c.getNetToughness() - c.getDamage();
            final int dmg = damageTo(c, combat);
            int value = 0;
            if (dmg >= left && dmg < left + def && c.canBeDestroyed()) {
                value += ComputerUtilCard.evaluateCreature(c);
            }
            final java.util.List<Card> foes = combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c);
            if (foes.size() == 1) {
                final Card foe = foes.get(0);
                if (c.getNetCombatDamage() < foe.getLethalDamage() && c.getNetCombatDamage() + att >= foe.getLethalDamage()) {
                    value += ComputerUtilCard.evaluateCreature(foe);
                }
            }
            if (combat.isAttacking(c) && (foes.isEmpty() || trample) && att > 0) {
                final Player opp = combat.getDefenderPlayerByAttacker(c);
                final int remaining = opp == null ? Integer.MAX_VALUE : forge.ai.ComputerUtilCombat.lifeThatWouldRemain(opp, combat);
                value += remaining > 0 && remaining <= att ? 1000 : att * 10;
            }
            if (value > bestValue) {
                bestValue = value;
                best = c;
            }
        }
        sa.resetTargets();
        if (best == null || bestValue < 100) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
