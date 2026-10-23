package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Blurry Visionary: the top two cards of the activator's library go back to back in one sleeve, either in front, and
 * become a modal double-faced card for the rest of the game, which goes to their hand. Only the outward face of a
 * double-faced card is kept; the other card ceases to exist. With one card left, it just goes to hand (rulings).
 * The AI keeps both front faces, the top card in front.
 */
public class SleeveEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Look at the top two cards of your library and put them back-to-back in the same sleeve. They become a modal double-faced card. Put it into your hand.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final CardCollection top = new CardCollection(p.getCardsIn(ZoneType.Library, 2));
        if (top.isEmpty()) {
            return;
        }
        if (top.size() == 1) {
            game.getAction().moveToHand(top.getFirst(), sa);
            return;
        }
        game.getAction().revealTo(top, p);
        final List<CardState> faces = Lists.newArrayList();
        for (final Card c : top) {
            faces.add(outwardFace(p, c, sa));
        }
        int front = 0;
        if (!p.getController().isAI()) {
            final List<String> names = Lists.newArrayList(faces.get(0).getName() + " (top card)", faces.get(1).getName() + " (second card)");
            final String pick = p.getController().chooseSomeType("face", sa, names, false);
            front = names.indexOf(pick) == 1 ? 1 : 0;
        }
        final Card frontCard = top.get(front);
        final Card backCard = top.get(1 - front);
        final CardState frontFace = faces.get(front);
        CardState frontSnap = null;
        if (frontFace != frontCard.getState(CardStateName.Original)) {
            frontSnap = new CardState(frontCard, CardStateName.Original);
            frontSnap.copyFrom(frontFace, false);
        }
        final CardState backSnap = new CardState(frontCard, CardStateName.Backside);
        backSnap.copyFrom(faces.get(1 - front), false);
        game.getAction().ceaseToExist(backCard, true);
        Card.applySleeve(frontCard, frontSnap, backSnap);
        game.getGameLog().add(GameLogEntryType.INFORMATION, p + " sleeves " + frontCard.getState(CardStateName.Original).getName()
                + " and " + backSnap.getName() + " together as a modal double-faced card.");
        game.getAction().moveToHand(frontCard, sa);
    }

    private static CardState outwardFace(final Player p, final Card c, final SpellAbility sa) {
        final CardState original = c.getState(CardStateName.Original);
        if (!(c.isTransformable() || c.isModal()) || !c.hasState(CardStateName.Backside) || p.getController().isAI()) {
            return original;
        }
        final CardState back = c.getState(CardStateName.Backside);
        final List<String> names = Lists.newArrayList(original.getName(), back.getName());
        final String pick = p.getController().chooseSomeType("face", sa, names, false);
        return back.getName().equals(pick) ? back : original;
    }
}
