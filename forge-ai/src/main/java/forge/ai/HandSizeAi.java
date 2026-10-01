package forge.ai;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.zone.ZoneType;

/**
 * Maro, Masumaro, Sturmgeist, Psychosis Crawler and friends are as big as their controller's hand. With one of them on
 * the battlefield every card played from hand shrinks it, so the AI keeps the hand for the creature and only plays
 * what's worth more than the counters it costs: removal and other interaction, cards that replace themselves,
 * creatures worth more than the shrink, a land when it's short of mana - and anything at all when the hand is over
 * its maximum size or the AI's life is in danger.
 */
public final class HandSizeAi {
    private HandSizeAi() { }

    /** How much power (and toughness) the AI's creatures lose for each card that leaves its hand. */
    public static int pointsPerCardInHand(final Player ai) {
        int points = 0;
        for (final Card c : ai.getCreaturesInPlay()) {
            for (final StaticAbility st : c.getStaticAbilities()) {
                if (!st.checkMode(StaticAbilityMode.Continuous) || !st.hasParam("CharacteristicDefining")
                        || !st.hasParam("SetPower")) {
                    continue;
                }
                final String count = c.getSVar(st.getParam("SetPower"));
                if (!count.startsWith("Count$ValidHand") && !count.startsWith("Count$InYourHand")) {
                    continue;
                }
                // "Count$ValidHand Card.YouOwn/Times.2": twice the number of cards in hand
                final int times = count.contains("/Times.") ? parse(count.substring(count.indexOf("/Times.") + 7)) : 1;
                points += Math.max(times, 1);
            }
        }
        return points;
    }

    private static int parse(final String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Whether playing this card from hand is worth what it takes off the AI's hand-size creatures. */
    public static boolean isWorthACardFromHand(final Player ai, final SpellAbility sa) {
        final Card card = sa.getHostCard();
        if (card == null || !card.isInZone(ZoneType.Hand)) {
            return true;
        }
        final int points = pointsPerCardInHand(ai);
        if (points == 0) {
            return true;
        }
        // over the maximum the card would be discarded at cleanup anyway
        if (!ai.isUnlimitedHandSize() && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize()) {
            return true;
        }
        if (ComputerUtil.aiLifeInDanger(ai, false, 0)) {
            return true;
        }
        if (sa.isLandAbility()) {
            return needsMoreMana(ai);
        }
        if (card.isCreature()) {
            // a hand point is a +1/+1 on a creature, which the evaluator values at about 25
            return ComputerUtilCard.evaluateCreature(card) >= points * 40;
        }
        if (card.isPlaneswalker()) {
            return true;
        }
        for (SpellAbility ab = sa; ab != null; ab = ab.getSubAbility()) {
            if (isInteraction(ai, ab)) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsMoreMana(final Player ai) {
        int most = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.isLand()) {
                most = Math.max(most, c.getCMC());
            }
        }
        return most > ComputerUtilMana.getAvailableManaEstimate(ai, false);
    }

    private static boolean isInteraction(final Player ai, final SpellAbility ab) {
        final ApiType api = ab.getApi();
        if (api == null) {
            return false;
        }
        switch (api) {
            case Destroy, DestroyAll, Counter, DealDamage, DamageAll, Sacrifice, SacrificeAll, Fight, GainControl,
                    Discard, Mill, Tap, TapAll, Token, CopyPermanent, Charm:
                return true;
            case ChangeZone, ChangeZoneAll:
                // removal (exile or bounce) or a tutor - not a card going back to the hand it came from
                return !"Hand".equals(ab.getParam("Origin")) || !"Battlefield".equals(ab.getParam("Destination"));
            case Draw, Dig, DigUntil:
                // replaces itself and then some
                return ab.hasParam("NumCards") && !"1".equals(ab.getParam("NumCards"));
            case Pump, PumpAll:
                // shrinking an opponent's creatures, or a trick while blockers are on the table
                return ab.isCurse() || ab.getParamOrDefault("NumAtt", "").startsWith("-")
                        || (ai.getGame().getCombat() != null
                            && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS));
            default:
                return false;
        }
    }
}
