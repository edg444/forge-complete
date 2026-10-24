package forge.ai.ability;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * AILogic$ HurtType (Outbreak, Pack's Disdain, Pact of the Serpent, Tsabo's Decree): "choose a creature type" for an
 * effect on that type, so every type in play (and, for Tsabo's Decree, in their hand) is tried for what the effect does
 * with it. The type it picked is the one chosen at resolution.
 */
public final class TypeHurtAi {

    private static final Map<Integer, String> CHOSEN = new ConcurrentHashMap<>();

    private TypeHurtAi() { }

    public static String chosenType(final Card host) {
        return host == null ? null : CHOSEN.get(host.getId());
    }

    private static int side(final Player ai, final Card c) {
        return c.getController().isOpponentOf(ai) ? 1 : c.getController().equals(ai) ? -1 : 0;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final SpellAbility effect = sa.getSubAbility();
        if (sa.getApi() != ApiType.ChooseType || effect == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Set<String> types = new LinkedHashSet<>();
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            types.addAll(c.getType().getCreatureTypes());
        }
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCardsIn(ZoneType.Hand)) {
                if (c.isCreature()) {
                    types.addAll(c.getType().getCreatureTypes());
                }
            }
        }
        int best = Integer.MIN_VALUE;
        String bestType = null;
        Object bestTarget = null;
        for (final String type : types) {
            int v = 0;
            Object target = null;
            final var ofType = CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), c -> c.isCreature() && c.getType().hasCreatureType(type));
            switch (effect.getApi()) {
                case PumpAll -> {
                    // Outbreak: -1/-1 to the type
                    final int n = Math.abs(Integer.parseInt(effect.getParamOrDefault("NumDef", "-1").replace("+", "")));
                    for (final Card c : ofType) {
                        if (c.getNetToughness() <= n) {
                            v += side(ai, c) * ComputerUtilCard.evaluateCreature(c);
                        }
                    }
                }
                case Pump -> {
                    // Pack's Disdain: -1/-1 for each permanent of the type it controls
                    final int n = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType(type)).size();
                    effect.resetTargets();
                    final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(effect),
                            c -> c.getController().isOpponentOf(ai) && c.getNetToughness() <= n));
                    if (t != null) {
                        v = ComputerUtilCard.evaluateCreature(t);
                        target = t;
                    }
                }
                case Discard -> {
                    // Tsabo's Decree: their creature cards of the type in hand, and their creatures of it
                    final Player opp = ai.getWeakestOpponent();
                    effect.resetTargets();
                    if (opp != null && effect.canTarget(opp)) {
                        v = (int) opp.getCardsIn(ZoneType.Hand).stream().filter(c -> c.isCreature() && c.getType().hasCreatureType(type)).count() * 50;
                        for (final Card c : ofType) {
                            if (c.getController().equals(opp) && c.canBeDestroyed()) {
                                v += ComputerUtilCard.evaluateCreature(c);
                            }
                        }
                        target = opp;
                    }
                }
                case Draw -> {
                    // Pact of the Serpent: draw X and lose X life, X its creatures of the type
                    effect.resetTargets();
                    final int mine = CardLists.filter(ai.getCreaturesInPlay(), c -> c.getType().hasCreatureType(type)).size();
                    if (effect.canTarget(ai) && mine >= 2 && ai.getLife() > mine + 4 && !ComputerUtil.aiLifeInDanger(ai, false, mine)) {
                        v = mine * 45;
                        target = ai;
                    }
                    for (final Player opp : ai.getOpponents()) {
                        final int theirs = CardLists.filter(opp.getCreaturesInPlay(), c -> c.getType().hasCreatureType(type)).size();
                        if (effect.canTarget(opp) && theirs >= opp.getLife() && opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife()) {
                            v = 1000;
                            target = opp;
                        }
                    }
                }
                default -> {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
            if (v > best) {
                best = v;
                bestType = type;
                bestTarget = target;
            }
        }
        if (bestType == null || best < 60) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        CHOSEN.put(sa.getHostCard().getId(), bestType);
        if (bestTarget instanceof forge.game.GameEntity e && effect.usesTargeting()) {
            effect.resetTargets();
            effect.getTargets().add(e);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
