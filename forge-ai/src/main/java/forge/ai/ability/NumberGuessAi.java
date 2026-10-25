package forge.ai.ability;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spells that name a number or a card type, the AI settling it when it decides and keeping to it as the spell
 * resolves.
 * <ul>
 * <li>AILogic$ VoidNumber (Void): the mana value whose artifacts and creatures cost them the most, less its own.</li>
 * <li>AILogic$ GuessHandType (Blood Oath, Fertile Imagination): the card type most common among the cards of theirs it
 * has seen (it doesn't look at their hand), when their hand should hold enough of it.</li>
 * <li>AILogic$ PrelateNumber (Sanctum Prelate, as it enters): the mana value their noncreature spells use most and its
 * own least.</li>
 * </ul>
 */
public final class NumberGuessAi {

    private static final Map<Integer, Integer> NUMBERS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> TYPES = new ConcurrentHashMap<>();
    private static final String[] CARD_TYPES = {"Land", "Creature", "Instant", "Sorcery", "Artifact", "Enchantment", "Planeswalker"};

    private NumberGuessAi() { }

    public static boolean handles(final String logic) {
        return "VoidNumber".equals(logic) || "GuessHandType".equals(logic);
    }

    /** The number the AI decided on for this card, if it decided on one. */
    public static Integer plannedNumber(final SpellAbility sa) {
        if ("PrelateNumber".equals(sa.getParam("AILogic"))) {
            return prelateNumber(sa.getActivatingPlayer() != null ? sa.getActivatingPlayer() : sa.getHostCard().getController());
        }
        return "VoidNumber".equals(sa.getRootAbility().getParam("AILogic")) ? NUMBERS.get(sa.getHostCard().getId()) : null;
    }

    /** The card type the AI decided on for this card, if it decided on one. */
    public static String plannedType(final SpellAbility sa) {
        return "GuessHandType".equals(sa.getRootAbility().getParam("AILogic")) ? TYPES.get(sa.getHostCard().getId()) : null;
    }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    private static boolean targetOpponent(final Player ai, final SpellAbility sa) {
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                sub.resetTargets();
                final Player opp = ai.getWeakestOpponent();
                if (opp == null || !sub.canTarget(opp)) {
                    return false;
                }
                sub.getTargets().add(opp);
            }
        }
        return true;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return "VoidNumber".equals(sa.getParam("AILogic")) ? voidNumber(ai, sa) : guessHand(ai, sa);
    }

    private static AiAbilityDecision voidNumber(final Player ai, final SpellAbility sa) {
        int best = 0;
        int bestN = -1;
        for (int n = 0; n <= 12; n++) {
            int v = 0;
            for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                if ((c.isCreature() || c.isArtifact()) && c.getCMC() == n && c.canBeDestroyed()) {
                    final int worth = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
                    v += c.getController().isOpponentOf(ai) ? worth : -worth;
                }
            }
            if (v > best) {
                best = v;
                bestN = n;
            }
        }
        if (bestN < 0 || best < 200 || !targetOpponent(ai, sa)) {
            return no(AiPlayDecision.CantPlayAi);
        }
        NUMBERS.put(sa.getHostCard().getId(), bestN);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    // the share of each card type among the cards of theirs it has seen
    private static double share(final Player opp, final String type) {
        final CardCollectionView seen = opp.getCardsIn(Arrays.asList(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile));
        if (seen.isEmpty()) {
            return "Land".equals(type) ? 0.4 : "Creature".equals(type) ? 0.35 : 0.1;
        }
        return (double) CardLists.count(seen, c -> c.getType().hasStringType(type)) / seen.size();
    }

    private static AiAbilityDecision guessHand(final Player ai, final SpellAbility sa) {
        final Player opp = ai.getWeakestOpponent();
        if (opp == null) {
            return no(AiPlayDecision.CantPlayAi);
        }
        String type = "Land";
        double best = -1;
        for (final String t : CARD_TYPES) {
            final double s = share(opp, t);
            if (s > best) {
                best = s;
                type = t;
            }
        }
        final double expected = best * opp.getCardsIn(ZoneType.Hand).size();
        boolean damage = false;
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            damage |= sub.getApi() == ApiType.DealDamage;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (damage) {
            // Blood Oath: 3 each, at their end step unless it's lethal now
            final boolean lethal = expected * 3 >= opp.getLife() && opp.canLoseLife();
            if (!lethal && (expected * 3 < 6 || !ph.is(PhaseType.END_OF_TURN) || ph.isPlayerTurn(ai))) {
                return no(AiPlayDecision.AnotherTime);
            }
        } else if (expected < 1.5 || !ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            // Fertile Imagination: two Saprolings each
            return no(AiPlayDecision.AnotherTime);
        }
        if (!targetOpponent(ai, sa)) {
            return no(AiPlayDecision.TargetingFailed);
        }
        TYPES.put(sa.getHostCard().getId(), type);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** Sanctum Prelate: the noncreature mana value they've shown most and its own deck has least. */
    public static int prelateNumber(final Player ai) {
        int best = Integer.MIN_VALUE;
        int bestN = 1;
        final CardCollectionView theirs = ai.getOpponents().getCardsIn(Arrays.asList(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile));
        final CardCollectionView mine = ai.getCardsIn(Arrays.asList(ZoneType.Library, ZoneType.Hand));
        for (int n = 0; n <= 8; n++) {
            final int k = n;
            final int score = 2 * CardLists.count(theirs, c -> !c.isCreature() && !c.isLand() && c.getCMC() == k)
                    - CardLists.count(mine, c -> !c.isCreature() && !c.isLand() && c.getCMC() == k);
            if (score > best || score == best && n == 1) {
                best = score;
                bestN = n;
            }
        }
        return bestN;
    }
}
