package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.Game;
import forge.game.ability.ApiType;
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
 * The last logics of the no-AI pass.
 * <ul>
 * <li>Garth (Garth One-Eye): a free copy from its list, in a main phase (the best of what's left is picked as it
 * resolves).</li>
 * <li>WakeSlaughter (Wake to Slaughter): its two best graveyard creatures, one to hand and one attacking with haste.</li>
 * <li>LifeDivided (Fire Covenant): life paid for X damage divided among their creatures, kills first, keeping 8 life.</li>
 * <li>NecroDraw (Necrodominance's end step): cards for life, up to a hand of seven, keeping 10 life.</li>
 * <li>HallowSpell (Hallow): their damage spell on the stack prevented, as life.</li>
 * <li>ColorFog (Prismatic Strands): once their attack is blocked, the color whose damage would hurt it most.</li>
 * </ul>
 */
public final class FinalCardAi {

    private FinalCardAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "Garth", "WakeSlaughter", "LifeDivided", "NecroDraw", "HallowSpell", "ColorFog" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "Garth" -> ownMain(ai) ? YES : no(AiPlayDecision.AnotherTime);
            case "WakeSlaughter" -> wake(ai, sa);
            case "LifeDivided" -> lifeDivided(ai, sa);
            case "NecroDraw" -> necro(ai, sa);
            case "HallowSpell" -> hallow(ai, sa);
            default -> colorFog(ai, sa);
        };
    }

    private static boolean ownMain(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)) && ai.getGame().getStack().isEmpty();
    }

    private static AiAbilityDecision wake(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final List<Card> mine = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getOwner().equals(ai)));
        mine.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c)).reversed());
        if (mine.size() < 2 || ComputerUtilCard.evaluateCreature(mine.get(0)) + ComputerUtilCard.evaluateCreature(mine.get(1)) < 300) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(mine.get(0));
        sa.getTargets().add(mine.get(1));
        return YES;
    }

    private static AiAbilityDecision lifeDivided(final Player ai, final SpellAbility sa) {
        int budget = ai.getLife() - 8;
        if (budget < 2) {
            return no(AiPlayDecision.CantAfford);
        }
        final List<Card> kills = new ArrayList<>(CardLists.filter(ai.getOpponents().getCreaturesInPlay(), c -> sa.canTarget(c) && c.canBeDestroyed()));
        kills.sort(Comparator.comparingInt((Card c) -> ComputerUtilCard.evaluateCreature(c) / Math.max(1, c.getLethalDamage())).reversed());
        sa.resetTargets();
        int x = 0;
        int value = 0;
        for (final Card c : kills) {
            final int need = c.getLethalDamage();
            if (need <= budget) {
                sa.getTargets().add(c);
                sa.addDividedAllocation(c, need);
                budget -= need;
                x += need;
                value += ComputerUtilCard.evaluateCreature(c);
            }
        }
        // a point of life is worth about 15
        if (x == 0 || value - x * 15 < 200) {
            sa.resetTargets();
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    private static AiAbilityDecision necro(final Player ai, final SpellAbility sa) {
        final int x = Math.min(7 - ai.getCardsIn(ZoneType.Hand).size(), ai.getLife() - 10);
        if (x < 1) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(x);
        return YES;
    }

    private static AiAbilityDecision hallow(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return no(AiPlayDecision.AnotherTime);
        }
        final SpellAbility top = game.getStack().peekAbility();
        boolean damages = false;
        for (SpellAbility s = top; s != null; s = s.getSubAbility()) {
            damages |= s.getApi() == ApiType.DealDamage || s.getApi() == ApiType.DamageAll;
        }
        if (!top.isSpell() || !top.getActivatingPlayer().isOpponentOf(ai) || !damages || !sa.canTargetSpellAbility(top)) {
            return no(AiPlayDecision.AnotherTime);
        }
        sa.resetTargets();
        sa.getTargets().add(top);
        return YES;
    }

    private static AiAbilityDecision colorFog(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || combat.getAttackingPlayer().equals(ai) || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return no(AiPlayDecision.WaitForCombat);
        }
        String best = null;
        int most = 0;
        for (final String color : forge.card.MagicColor.Constant.ONLY_COLORS) {
            final byte mask = forge.card.MagicColor.fromName(color);
            int dmg = 0;
            for (final Card a : combat.getAttackersOf(ai)) {
                if (a.getColor().hasAnyColor(mask)) {
                    dmg += a.getNetCombatDamage();
                }
            }
            if (dmg > most) {
                most = dmg;
                best = color;
            }
        }
        if (best == null || most < 4 && !ComputerUtilCombat.lifeInDanger(ai, combat)) {
            return no(AiPlayDecision.DoesntImpactCombat);
        }
        MiscSpellAi.planColor(sa.getHostCard(), best);
        return YES;
    }
}
