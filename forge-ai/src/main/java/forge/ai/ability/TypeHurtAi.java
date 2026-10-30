package forge.ai.ability;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiTeams;
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
        return AiTeams.sign(ai, c.getController());
    }

    // Winding Way, Vigean Intuition: the type its library has the most of, for the cards revealed off the top
    private static AiAbilityDecision digType(final Player ai, final SpellAbility sa, final SpellAbility effect) {
        final var library = ai.getCardsIn(ZoneType.Library);
        if (library.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final String[] types = sa.hasParam("ValidTypes") ? sa.getParam("ValidTypes").split(",")
                : new String[] {"Creature", "Land", "Instant", "Sorcery", "Artifact", "Enchantment", "Planeswalker"};
        final int dig = Math.min(library.size(), forge.game.ability.AbilityUtils.calculateAmount(sa.getHostCard(), effect.getParamOrDefault("DigNum", "1"), effect));
        int best = 0;
        String bestType = null;
        for (final String type : types) {
            final long n = library.stream().filter(c -> c.getType().hasStringType(type)).count();
            // the expected number of them in the cards dug, at half a card's worth for a land
            final int v = (int) (n * dig * ("Land".equals(type) ? 25 : 50) / library.size());
            if (v > best) {
                best = v;
                bestType = type;
            }
        }
        if (bestType == null || best < 75) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        CHOSEN.put(sa.getHostCard().getId(), bestType);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // Creeping Renaissance: the permanent type that brings back the most from its graveyard
    private static AiAbilityDecision cardType(final Player ai, final SpellAbility sa, final SpellAbility effect) {
        if (effect.getApi() == ApiType.Dig) {
            return digType(ai, sa, effect);
        }
        if (effect.getApi() != ApiType.ChangeZoneAll || !effect.getParamOrDefault("Origin", "").contains("Graveyard")) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        int best = 0;
        String bestType = null;
        for (final String type : new String[] {"Creature", "Artifact", "Enchantment", "Land", "Planeswalker", "Battle"}) {
            int v = 0;
            for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
                if (c.getType().hasStringType(type)) {
                    v += c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : c.isLand() ? 40 : 30 + c.getCMC() * 25;
                }
            }
            if (v > best) {
                best = v;
                bestType = type;
            }
        }
        if (bestType == null || best < 200) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        CHOSEN.put(sa.getHostCard().getId(), bestType);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final SpellAbility effect = sa.getSubAbility();
        if (sa.getApi() != ApiType.ChooseType || effect == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if ("Card".equals(sa.getParam("Type"))) {
            return cardType(ai, sa, effect);
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
                case DealDamage -> {
                    // Coordinated Barrage: damage for each permanent of the type it controls
                    final int n = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType(type)).size();
                    effect.resetTargets();
                    final Card t = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(effect),
                            c -> c.getController().isOpponentOf(ai) && c.getLethalDamage() <= n && c.canBeDestroyed()));
                    if (t != null) {
                        v = ComputerUtilCard.evaluateCreature(t);
                        target = t;
                    }
                    // Roar of the Crowd: any target, so the player too
                    for (final Player opp : ai.getOpponents()) {
                        if (n > 0 && effect.canTarget(opp) && opp.canLoseLife()) {
                            final int pv = n >= opp.getLife() && !opp.cantLoseForZeroOrLessLife() ? 1000 : n * 12;
                            if (pv > v) {
                                v = pv;
                                target = opp;
                            }
                        }
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
