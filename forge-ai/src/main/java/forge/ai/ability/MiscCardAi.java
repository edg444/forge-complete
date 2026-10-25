package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * More one-card logics for the last of the no-AI cards.
 * <ul>
 * <li>HandAttack (Wistful Thinking): their hand, two or more cards, in its second main phase.</li>
 * <li>AugurDraw (Corpse Augur, as it dies): the player whose graveyard gives the most cards it can afford in life.</li>
 * <li>DoublePower (Soul's Might): its creature with the most power, 3 or more.</li>
 * <li>AuraBarbs (Aura Barbs): when their enchantments and enchanted creatures take more of it than its own.</li>
 * <li>CoupSteal (Confiscation Coup): their best artifact or creature its energy covers.</li>
 * <li>EnergyTick / EnergyDestroy (HELIOS One): an energy at their end step; then their best nonland permanent whose mana
 * value the energy matches.</li>
 * <li>HavenSave / HavenReturn (Safe Haven, Synod Sanctum): its permanent exiled ahead of their removal, back once the
 * danger has passed.</li>
 * <li>Juxtapose (Juxtapose): when their greatest creature and artifact are worth more than its own.</li>
 * </ul>
 */
public final class MiscCardAi {

    private MiscCardAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        // Juxtapose's WorstCard is the opponent's choice as it resolves; casting it is this
        if ("WorstCard".equals(logic) && sa.getApi() == ApiType.ChooseCard && sa.usesTargeting() && sa.hasParam("TargetControls")
                && sa.getSubAbility() != null) {
            return true;
        }
        return switch (logic) {
            case "HandAttack", "AugurDraw", "DoublePower", "AuraBarbs", "CoupSteal", "EnergyTick", "EnergyDestroy", "HavenSave",
                    "HavenReturn" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParamOrDefault("AILogic", "")) {
            case "HandAttack" -> handAttack(ai, sa);
            case "AugurDraw" -> augur(ai, sa);
            case "DoublePower" -> doublePower(ai, sa);
            case "AuraBarbs" -> auraBarbs(ai, sa);
            case "CoupSteal" -> coup(ai, sa);
            case "EnergyTick" -> theirEnd(ai) ? YES : no(AiPlayDecision.AnotherTime);
            case "EnergyDestroy" -> energyDestroy(ai, sa);
            case "HavenSave" -> havenSave(ai, sa);
            case "HavenReturn" -> havenReturn(ai, sa);
            default -> juxtapose(ai, sa);
        };
    }

    private static boolean theirEnd(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static boolean ownMain(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2));
    }

    private static int worth(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
    }

    private static AiAbilityDecision handAttack(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        if (opp == null || !sa.canTarget(opp) || opp.getCardsIn(ZoneType.Hand).size() < 2) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }

    private static AiAbilityDecision augur(final Player ai, final SpellAbility sa) {
        Player best = null;
        int bestN = -1;
        Player fewest = null;
        int fewestN = Integer.MAX_VALUE;
        for (final Player p : ai.getGame().getPlayers()) {
            if (!sa.canTarget(p)) {
                continue;
            }
            final int n = CardLists.count(p.getCardsIn(ZoneType.Graveyard), Card::isCreature);
            if (n < ai.getLife() - 3 && n > bestN) {
                best = p;
                bestN = n;
            }
            if (n < fewestN) {
                fewest = p;
                fewestN = n;
            }
        }
        final Player pick = best != null ? best : fewest;
        if (pick == null) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(pick);
        return YES;
    }

    private static AiAbilityDecision doublePower(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Card best = CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai)).stream()
                .max(java.util.Comparator.comparingInt(Card::getNetPower)).orElse(null);
        if (best == null || best.getNetPower() < 3) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision auraBarbs(final Player ai, final SpellAbility sa) {
        int v = 0;
        int ownEnchantments = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.isEnchantment()) {
                final boolean theirs = c.getController().isOpponentOf(ai);
                v += theirs ? 20 : -20;
                ownEnchantments += theirs ? 0 : 1;
                final Card on = c.isAura() ? c.getAttachedTo() : null;
                if (on != null && on.isCreature() && on.getLethalDamage() <= 2 && on.canBeDestroyed()) {
                    v += (on.getController().isOpponentOf(ai) ? 1 : -1) * ComputerUtilCard.evaluateCreature(on);
                }
            }
        }
        if (ai.getLife() <= ownEnchantments * 2 + 3) {
            return no(AiPlayDecision.CantPlayAi);
        }
        return v >= 100 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision coup(final Player ai, final SpellAbility sa) {
        final SpellAbility steal = sa.getSubAbility();
        if (steal == null || !steal.usesTargeting()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final int energy = ai.getCounters(CounterEnumType.ENERGY) + 4;
        final Card best = CardUtil.getValidCardsToTarget(steal).stream()
                .filter(c -> c.getController().isOpponentOf(ai) && c.getCMC() <= energy)
                .max(java.util.Comparator.comparingInt(MiscCardAi::worth)).orElse(null);
        if (best == null || worth(best) < 150) {
            return no(AiPlayDecision.TargetingFailed);
        }
        steal.resetTargets();
        steal.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision energyDestroy(final Player ai, final SpellAbility sa) {
        final int energy = ai.getCounters(CounterEnumType.ENERGY);
        Card best = null;
        for (final Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() || c.getCMC() > energy) {
                continue;
            }
            sa.setXManaCostPaid(c.getCMC());
            if (sa.canTarget(c) && (best == null || worth(c) > worth(best))) {
                best = c;
            }
        }
        if (best == null || worth(best) < 150) {
            sa.setXManaCostPaid(null);
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.setXManaCostPaid(best.getCMC());
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    private static AiAbilityDecision havenSave(final Player ai, final SpellAbility sa) {
        if (ai.getGame().getStack().isEmpty()) {
            return no(AiPlayDecision.AnotherTime);
        }
        final List<GameObject> doomed = ComputerUtil.predictThreatenedObjects(ai, null);
        final Card save = CardUtil.getValidCardsToTarget(sa).stream().filter(c -> c.getController().equals(ai) && doomed.contains(c))
                .max(java.util.Comparator.comparingInt(MiscCardAi::worth)).orElse(null);
        if (save == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(save);
        return YES;
    }

    private static AiAbilityDecision havenReturn(final Player ai, final SpellAbility sa) {
        final boolean any = ai.getGame().getCardsIn(ZoneType.Exile).anyMatch(c -> sa.getHostCard().equals(c.getExiledWith()));
        if (!any || !ai.getGame().getStack().isEmpty()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        // Safe Haven's upkeep trigger, or Synod Sanctum once its own turn is safe
        return sa.isTrigger() || ownMain(ai) ? YES : no(AiPlayDecision.AnotherTime);
    }

    private static Card greatest(final Player p, final java.util.function.Predicate<Card> kind) {
        return p.getCardsIn(ZoneType.Battlefield).stream().filter(kind).max(java.util.Comparator.comparingInt(Card::getCMC)).orElse(null);
    }

    private static AiAbilityDecision juxtapose(final Player ai, final SpellAbility sa) {
        if (!ownMain(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        if (opp == null || !sa.canTarget(opp)) {
            return no(AiPlayDecision.TargetingFailed);
        }
        int gain = 0;
        for (final java.util.function.Predicate<Card> kind : List.<java.util.function.Predicate<Card>>of(Card::isCreature, Card::isArtifact)) {
            final Card theirs = greatest(opp, kind);
            final Card mine = greatest(ai, kind);
            if (theirs != null && mine != null) {
                gain += worth(theirs) - worth(mine);
            }
        }
        // a swap costs the card, so a clear step up
        if (gain < 70) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }
}
