package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * One-card spell and ability logics for the last of the no-AI cards.
 * <ul>
 * <li>GraveCastEnable (Chainer, Nightmare Adept): a spare card discarded when a creature in its graveyard is worth
 * casting with the mana it has.</li>
 * <li>BriefingCast (Mission Briefing): in a main phase, with mana left for the best instant or sorcery in its
 * graveyard.</li>
 * <li>OppLibraryCast (Knowledge Exploitation): their library's best instant or sorcery, in its second main phase.</li>
 * <li>CascadeOnly (Throes of Chaos): the spell does nothing but cascade, so in a main phase.</li>
 * <li>BlastCharge / BlastFire (Blast Zone): counters up to the mana value that costs them the most, then the sweep.</li>
 * <li>Confluence (Mystic Confluence): their spell countered and two cards, or three cards at their end step.</li>
 * <li>FlashCheat (Flash): the best creature in hand whose reduced cost it can pay, at their end step or before
 * their blocks.</li>
 * <li>WipeEnchantColor (Root Greevil): the color whose enchantments cost them the most.</li>
 * <li>Shapeshift (Riptide Shapeshifter): when the average creature in its library beats this 2/2.</li>
 * <li>JesterMask (Jester's Mask): their hand of four or more swapped for their worst cards.</li>
 * </ul>
 */
public final class MiscSpellAi {

    private static final Map<Integer, String> COLORS = new ConcurrentHashMap<>();

    private MiscSpellAi() { }

    private static final AiAbilityDecision YES = new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    public static boolean handles(final String logic) {
        return switch (logic == null ? "" : logic) {
            case "GraveCastEnable", "BriefingCast", "OppLibraryCast", "CascadeOnly", "BlastCharge", "BlastFire", "Confluence",
                    "FlashCheat", "WipeEnchantColor", "Shapeshift", "JesterMask", "CopyPower", "CouncilVote" -> true;
            default -> false;
        };
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return switch (sa.getParam("AILogic")) {
            case "GraveCastEnable" -> graveCast(ai, sa);
            case "BriefingCast" -> briefing(ai, sa);
            case "OppLibraryCast" -> oppLibrary(ai, sa);
            case "CascadeOnly" -> cascade(ai, sa);
            case "BlastCharge" -> blastCharge(ai, sa);
            case "BlastFire" -> blastFire(ai, sa);
            case "Confluence" -> confluence(ai, sa);
            case "FlashCheat" -> flash(ai, sa);
            case "WipeEnchantColor" -> wipeColor(ai, sa);
            case "Shapeshift" -> shapeshift(ai, sa);
            case "CopyPower" -> copyPower(ai, sa);
            case "CouncilVote" -> councilVote(ai, sa);
            default -> jesterMask(ai, sa);
        };
    }

    private static boolean mainPhase(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)) && ai.getGame().getStack().isEmpty();
    }

    private static int mana(final Player ai, final SpellAbility sa) {
        final int cost = sa.getPayCosts() == null || sa.getPayCosts().getTotalMana() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
        return ComputerUtilMana.getAvailableManaEstimate(ai, false) - cost;
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

    private static AiAbilityDecision graveCast(final Player ai, final SpellAbility sa) {
        if (!mainPhase(ai) || ai.getCardsIn(ZoneType.Hand).size() < 2) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int mana = mana(ai, sa);
        final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCardsIn(ZoneType.Graveyard),
                c -> c.isCreature() && c.getCMC() <= mana));
        final int cost = spare(ai, sa.getHostCard()) > 0 ? 40 : 120;
        return best != null && ComputerUtilCard.evaluateCreature(best) - cost >= 100 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision briefing(final Player ai, final SpellAbility sa) {
        if (!mainPhase(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int mana = mana(ai, sa);
        final boolean any = ai.getCardsIn(ZoneType.Graveyard).anyMatch(c -> (c.isInstant() || c.isSorcery()) && c.getCMC() >= 2 && c.getCMC() <= mana);
        return any ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision oppLibrary(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        if (opp == null || !sa.canTarget(opp) || opp.getCardsIn(ZoneType.Library).size() < 10) {
            return no(AiPlayDecision.TargetingFailed);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        return YES;
    }

    private static AiAbilityDecision cascade(final Player ai, final SpellAbility sa) {
        return mainPhase(ai) ? YES : no(AiPlayDecision.AnotherTime);
    }

    // what destroying every nonland permanent of mana value n does, for it
    private static int blastValue(final Player ai, final int n) {
        int v = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() && c.getCMC() == n) {
                final int w = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                v += c.getController().isOpponentOf(ai) ? w : -w;
            }
        }
        return v;
    }

    private static AiAbilityDecision blastCharge(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int now = sa.getHostCard().getCounters(forge.game.card.CounterEnumType.CHARGE);
        sa.setXManaCostPaid(null);
        final int max = ComputerUtilMana.determineLeftoverMana(sa, ai, false);
        int best = Math.max(200, blastValue(ai, now));
        int bestX = 0;
        for (int x = 1; x <= max; x++) {
            final int v = blastValue(ai, now + x);
            if (v > best) {
                best = v;
                bestX = x;
            }
        }
        if (bestX == 0) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.setXManaCostPaid(bestX);
        return YES;
    }

    private static AiAbilityDecision blastFire(final Player ai, final SpellAbility sa) {
        final int now = sa.getHostCard().getCounters(forge.game.card.CounterEnumType.CHARGE);
        return blastValue(ai, now) >= 200 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision confluence(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        AbilitySub counter = null;
        AbilitySub draw = null;
        for (final AbilitySub sub : forge.game.ability.effects.CharmEffect.makePossibleOptions(sa)) {
            if (sub.getApi() == ApiType.Counter) {
                counter = sub;
            } else if (sub.getApi() == ApiType.Draw) {
                draw = sub;
            }
        }
        if (draw == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final List<AbilitySub> chosen = new ArrayList<>();
        if (counter != null && !ai.getGame().getStack().isEmpty()) {
            final SpellAbility top = ai.getGame().getStack().peekAbility();
            final Player them = top.getActivatingPlayer();
            if (top.isSpell() && them.isOpponentOf(ai) && counter.canTargetSpellAbility(top)
                    && ComputerUtilMana.getAvailableManaEstimate(them, false) < 3) {
                counter.setActivatingPlayer(ai);
                counter.resetTargets();
                counter.getTargets().add(top);
                chosen.add(counter);
            }
        }
        if (chosen.isEmpty() && !(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai))) {
            return no(AiPlayDecision.AnotherTime);
        }
        while (chosen.size() < 3) {
            chosen.add(draw);
        }
        sa.setChosenList(chosen);
        return YES;
    }

    private static AiAbilityDecision flash(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean timing = !ph.isPlayerTurn(ai) && (ph.is(PhaseType.END_OF_TURN) || ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS));
        if (!timing) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int mana = mana(ai, sa);
        final Card best = ComputerUtilCard.getBestCreatureAI(CardLists.filter(ai.getCardsIn(ZoneType.Hand),
                c -> c.isCreature() && Math.max(0, c.getCMC() - 2) <= mana && c.getCMC() >= 3));
        if (best == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        ZonePickAi.plan(sa.getHostCard(), List.of(best));
        return YES;
    }

    private static AiAbilityDecision wipeColor(final Player ai, final SpellAbility sa) {
        String best = null;
        int bestV = 0;
        for (final String color : forge.card.MagicColor.Constant.ONLY_COLORS) {
            final byte mask = forge.card.MagicColor.fromName(color);
            int v = 0;
            for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                if (c.isEnchantment() && c.getColor().hasAnyColor(mask)) {
                    v += c.getController().isOpponentOf(ai) ? ExchangeTradeAi.value(c) : -ExchangeTradeAi.value(c);
                }
            }
            if (v > bestV) {
                bestV = v;
                best = color;
            }
        }
        if (best == null || bestV < 100) {
            return no(AiPlayDecision.CantPlayAi);
        }
        COLORS.put(sa.getHostCard().getId(), best);
        return YES;
    }

    /** Root Greevil: the color it decided on. */
    public static String plannedColor(final SpellAbility sa) {
        return "WipeEnchantColor".equals(sa.getRootAbility().getParam("AILogic")) ? COLORS.get(sa.getHostCard().getId()) : null;
    }

    private static AiAbilityDecision shapeshift(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final CardCollectionView lib = ai.getCardsIn(ZoneType.Library);
        final List<Card> creatures = CardLists.filter(lib, Card::isCreature);
        if (creatures.size() < 4) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final double avg = creatures.stream().mapToInt(ComputerUtilCard::evaluateCreature).average().orElse(0);
        return avg - ComputerUtilCard.evaluateCreature(sa.getHostCard()) >= 80 ? YES : no(AiPlayDecision.CantPlayAi);
    }

    // Riptide Mangler: the biggest power on the battlefield, when it beats its own by 2 or more
    private static AiAbilityDecision copyPower(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Card best = forge.game.card.CardUtil.getValidCardsToTarget(sa).stream().filter(c -> !c.equals(host))
                .max(java.util.Comparator.comparingInt(Card::getNetPower)).orElse(null);
        if (best == null || best.getNetPower() < host.getNetPower() + 2 || !mainPhase(ai)) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(best);
        return YES;
    }

    // Bite of the Black Rose: against one opponent the vote ties at worst, which is their two discards
    private static AiAbilityDecision councilVote(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final Player opp = ai.getStrongestOpponent();
        final boolean sickness = opp != null && CardLists.count(opp.getCreaturesInPlay(), c -> c.getNetToughness() <= 2) >= 2;
        return opp != null && (opp.getCardsIn(ZoneType.Hand).size() >= 2 || sickness) ? YES : no(AiPlayDecision.CantPlayAi);
    }

    private static AiAbilityDecision jesterMask(final Player ai, final SpellAbility sa) {
        final Player opp = ai.getStrongestOpponent();
        if (opp == null || !sa.canTarget(opp) || opp.getCardsIn(ZoneType.Hand).size() < 4) {
            return no(AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(opp);
        ZonePickAi.planWorst(sa.getHostCard());
        return YES;
    }
}
