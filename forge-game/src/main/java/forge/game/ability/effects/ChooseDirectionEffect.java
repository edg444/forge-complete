package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.game.Direction;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class ChooseDirectionEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        // left/right isn't always about seating order - Farewell to Arms picks one of a player's
        // hands, where a list of who sits clockwise would only confuse
        if (!sa.hasParam("NoTurnOrderInfo")) {
            final FCollection<Player> left = new FCollection<>(game.getPlayers());
            // TODO: We'd better set up turn order UI here
            final String info = Localizer.getInstance().getMessage("lblLeftClockwise") + ": " + left + "\r\n" + Localizer.getInstance().getMessage("lblRightAntiClockwise") + ":" + Lists.reverse(left);
            sa.getActivatingPlayer().getController().notifyOfValue(sa, source, info);
        }

        final String prompt = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle")
                : Localizer.getInstance().getMessage("lblChooseDirection");
        boolean chosen = sa.getActivatingPlayer().getController().chooseBinary(sa,
                prompt, BinaryChoiceType.LeftOrRight);
        source.setChosenDirection(chosen ? Direction.Left : Direction.Right);
    }
}
