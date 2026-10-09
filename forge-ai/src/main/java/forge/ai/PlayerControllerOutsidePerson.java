package forge.ai;

import java.util.List;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.OutsidePlayers;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * A person from outside the game making a player's decisions for a turn (Kindslaver). User, 2026-10-09: one of three
 * kinds at random -
 * <ul>
 * <li>HELPFUL plays aggressively in that player's interest: the normal AI, on its Reckless profile (set on the lobby
 * player by the factory).</li>
 * <li>NOVICE doesn't play Magic and just says "go" (rulings): plays no land and casts nothing, doesn't attack, and makes
 * the choices the game forces on it at random.</li>
 * <li>SABOTEUR plays aggressively against that player: no land, nothing cast, every creature attacks every combat, and
 * whatever it's forced to discard or sacrifice is the best it has.</li>
 * </ul>
 */
public class PlayerControllerOutsidePerson extends PlayerControllerAi {
    private final OutsidePlayers.Style style;

    public PlayerControllerOutsidePerson(final Game game, final Player p, final LobbyPlayer lp, final OutsidePlayers.Style style) {
        super(game, p, lp);
        this.style = style;
    }

    public OutsidePlayers.Style getStyle() {
        return style;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return style == OutsidePlayers.Style.HELPFUL ? super.chooseSpellAbilityToPlay() : null;
    }

    @Override
    public void declareAttackers(final Player attacker, final Combat combat) {
        switch (style) {
            case HELPFUL:
                super.declareAttackers(attacker, combat);
                break;
            case NOVICE:
                break;
            case SABOTEUR:
                for (final Card c : attacker.getCreaturesInPlay()) {
                    for (final GameEntity defender : combat.getDefenders()) {
                        if (defender instanceof Player && CombatUtil.canAttack(c, defender)) {
                            combat.addAttacker(c, defender);
                            break;
                        }
                    }
                }
                // an all-out attack can break a rule ("can't attack alone"...) - then the AI's own legal attack instead,
                // or the declaration would be asked for again and again
                if (!CombatUtil.validateAttackers(combat)) {
                    for (final Card c : CardLists.filterControlledBy(combat.getAttackers(), attacker)) {
                        combat.removeFromCombat(c);
                    }
                    super.declareAttackers(attacker, combat);
                }
                break;
        }
    }

    /** The best of these first - what a saboteur parts with. */
    private static CardCollection best(final CardCollectionView cards, final int n) {
        final CardCollection sorted = new CardCollection(cards);
        sorted.sort((a, b) -> ComputerUtilCard.evaluateCardImpact(b) - ComputerUtilCard.evaluateCardImpact(a));
        return new CardCollection(sorted.subList(0, Math.min(n, sorted.size())));
    }

    private static CardCollection random(final CardCollectionView cards, final int n) {
        return new CardCollection(Aggregates.random(cards, Math.min(n, cards.size())));
    }

    @Override
    public CardCollectionView chooseCardsToDiscardToMaximumHandSize(final int numDiscard) {
        final CardCollectionView hand = getPlayer().getCardsIn(forge.game.zone.ZoneType.Hand);
        switch (style) {
            case NOVICE:
                return random(hand, numDiscard);
            case SABOTEUR:
                return best(hand, numDiscard);
            default:
                return super.chooseCardsToDiscardToMaximumHandSize(numDiscard);
        }
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(final Player p, final SpellAbility sa, final CardCollection validCards,
            final int min, final int max, final CardCollectionView visibleToChooser) {
        if (p == getPlayer()) {
            switch (style) {
                case NOVICE:
                    return random(validCards, min);
                case SABOTEUR:
                    return best(validCards, max);
                default:
                    break;
            }
        }
        return super.chooseCardsToDiscardFrom(p, sa, validCards, min, max, visibleToChooser);
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(final SpellAbility sa, final int min, final int max,
            final CardCollectionView validTargets, final String message) {
        switch (style) {
            case NOVICE:
                return random(validTargets, min);
            case SABOTEUR:
                return best(validTargets, max);
            default:
                return super.choosePermanentsToSacrifice(sa, min, max, validTargets, message);
        }
    }
}
