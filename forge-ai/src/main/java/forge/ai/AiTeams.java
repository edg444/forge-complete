package forge.ai;

import java.util.HashSet;
import java.util.Set;

import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sides for weighing symmetric effects. Teammates win and lose together (team games, Two-Headed Giant, the
 * archenemy's opponents), so what an effect costs a teammate it costs the AI, and only opposing players' losses
 * are gains. In a free-for-all every opponent is a side of its own, and an effect that hurts them all evenly only
 * gains the AI ground against each one, which is why comparisons scale the AI's loss by {@link #opposingSides}.
 */
public final class AiTeams {
    private AiTeams() {
    }

    /** The AI and its teammates. */
    public static PlayerCollection side(final Player ai) {
        return ai.getYourTeam();
    }

    /** One per opposing team, plus one per opponent without a team; never less than one. */
    public static int opposingSides(final Player ai) {
        final Set<Integer> teams = new HashSet<>();
        int solo = 0;
        for (final Player opp : ai.getOpponents()) {
            if (opp.getTeam() < 0) {
                solo++;
            } else {
                teams.add(opp.getTeam());
            }
        }
        return Math.max(1, solo + teams.size());
    }

    /**
     * How a change to something a player controls counts for the AI: +1 for an opponent, -1 for the AI or a teammate,
     * 0 for a player who is neither (the interplanar battlefield's holder is nobody's opponent).
     */
    public static int sign(final Player ai, final Player p) {
        if (p == null) {
            return 0;
        }
        if (p.isOpponentOf(ai)) {
            return 1;
        }
        return side(ai).contains(p) ? -1 : 0;
    }

    /** The valid cards in a zone across the AI's side. */
    public static CardCollection sideCards(final Player ai, final ZoneType zone, final String valid, final SpellAbility sa) {
        return CardLists.getValidCards(side(ai).getCardsIn(zone), valid, sa.getActivatingPlayer(), sa.getHostCard(), sa);
    }

    /** The valid cards in a zone across all opponents. */
    public static CardCollection opponentCards(final Player ai, final ZoneType zone, final String valid, final SpellAbility sa) {
        return CardLists.getValidCards(ai.getOpponents().getCardsIn(zone), valid, sa.getActivatingPlayer(), sa.getHostCard(), sa);
    }

    /** The creatures in a list that the AI's side controls. */
    public static CardCollection sideCreatures(final Player ai, final CardCollectionView list) {
        return CardLists.filter(list, CardPredicates.isControlledByAnyOf(side(ai)), CardPredicates.CREATURES);
    }

    /**
     * Whether an effect hurting both sides is worth it: the opponents' combined loss has to beat the AI side's loss
     * once for every opposing side, plus a margin.
     */
    public static boolean outweighs(final int opponentsLoss, final int sideLoss, final int sides, final int margin) {
        return opponentsLoss > sideLoss * sides + margin;
    }

    /** The lowest life on the AI's side: the player a symmetric life loss endangers first. */
    public static int lowestSideLife(final Player ai) {
        int lowest = Integer.MAX_VALUE;
        for (final Player p : side(ai)) {
            lowest = Math.min(lowest, p.getLife());
        }
        return lowest;
    }

    /**
     * How much of a symmetric "each player loses N" hits one shared life total: Two-Headed Giant heads share one,
     * so it loses N once per head (CR 810.9).
     */
    public static int sharedLifeHits(final Player p) {
        return p.isOnGiantTeam() ? p.getGiantTeam().size() : 1;
    }
}
