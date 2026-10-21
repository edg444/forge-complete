package forge.game.ability.effects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardView;
import forge.game.event.GameEventFlipOntoBattlefield;
import forge.game.event.TossStyle;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * Devil K. Nevil: flicked off the table edge over a line of any number of the Choices$ creatures. There's no table, so the
 * jump is simulated (user, 2026-10-21): it clears one creature 85% of the time, 12% less for each one more, never under
 * 10%, shown with the dexterity toss animation. Jumping none is no attempt (rulings). The creatures cleared are left in
 * the host's Cleared SVar for the counters.
 */
public class JumpOverEffect extends SpellAbilityEffect {

    public static int clearChance(final int creatures) {
        return Math.max(10, 85 - 12 * (creatures - 1));
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard() + " jumps over creatures.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("Cleared", "Number$0");
        final CardCollection line = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParam("Choices"), you, host, sa);
        line.remove(host);
        if (line.isEmpty()) {
            return;
        }
        final int n = you.getController().chooseNumber(sa,
                "Jump " + host + " over how many creatures? (The more, the harder; 0 is no jump.)", 0, line.size());
        if (n <= 0) {
            return;
        }
        final List<Card> jumped = Aggregates.random(line, n);
        final boolean cleared = MyRandom.percentTrue(clearChance(n));

        // the game decides what it lands on: past the line, or short of it
        final TossStyle style = new TossStyle(-1, 0, true, false, false);
        final CardView farthest = jumped.get(jumped.size() - 1).getView();
        final List<CardView> touched = new CopyOnWriteArrayList<>();
        if (!cleared) {
            touched.add(farthest);
        }
        game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), farthest, touched, 1, false, style));
        game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), farthest, touched, 1, true, style));

        final String result = host + (cleared ? " clears " : " doesn't clear ") + n + (n == 1 ? " creature" : " creatures")
                + (cleared ? "!" : ". Shame, and no counters.");
        game.getGameLog().add(GameLogEntryType.INFORMATION, result);
        game.getAction().notifyOfValue(sa, host, result, null);
        if (cleared) {
            host.setSVar("Cleared", "Number$" + n);
        }
    }
}
