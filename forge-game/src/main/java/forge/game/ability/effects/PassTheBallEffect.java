package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * All-Star Kicker: "your team may pass the ball" - one player on your team gives a permanent they control or a card in
 * their hand to another player on your team (a permanent changes control; a card goes to that player's hand, still
 * owned by its owner). With no teammates there's no one to pass to (rulings).
 */
public class PassTheBallEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Your team may pass the ball.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final PlayerCollection team = new PlayerCollection(you.getYourTeam());
        if (!team.contains(you)) {
            team.add(you);
        }
        if (team.size() < 2 || !you.getController().confirmAction(sa, null, "Pass the ball?", null)) {
            return;
        }
        final Player giver = you.getController().chooseSingleEntityForEffect(team, sa, "Choose who passes the ball", null);
        if (giver == null) {
            return;
        }
        final List<GameEntity> balls = Lists.newArrayList();
        balls.addAll(giver.getCardsIn(ZoneType.Battlefield));
        balls.addAll(giver.getCardsIn(ZoneType.Hand));
        if (balls.isEmpty()) {
            return;
        }
        final CardCollection options = new CardCollection();
        for (final GameEntity e : balls) {
            options.add((Card) e);
        }
        final Card ball = giver.getController().chooseSingleEntityForEffect(options, sa, "Choose a permanent or card in hand to pass", null);
        final PlayerCollection receivers = new PlayerCollection(team);
        receivers.remove(giver);
        final Player receiver = giver.getController().chooseSingleEntityForEffect(receivers, sa, "Pass the ball to", null);
        if (ball == null || receiver == null) {
            return;
        }
        if (ball.isInPlay()) {
            ball.setController(receiver, game.getNextTimestamp());
        } else {
            game.getAction().moveTo(receiver.getZone(ZoneType.Hand), ball, sa, null);
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, giver + " passes " + ball + " to " + receiver + ".");
    }
}
