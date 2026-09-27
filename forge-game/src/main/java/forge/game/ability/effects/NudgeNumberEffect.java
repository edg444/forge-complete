package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.NumberInstances;
import forge.game.card.NumberNudge;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * More or Less: "Add or subtract 1 or one from a number or number word on target spell or permanent until end
 * of turn." The chooser picks one of the numbers printed on it (see {@link NumberInstances}), then up or down.
 */
public class NudgeNumberEffect extends SpellAbilityEffect {

    public static final String ADD = "Add 1";
    public static final String SUBTRACT = "Subtract 1";

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Add or subtract 1 from a number on " + Lists.newArrayList(getTargetCards(sa)) + " until end of turn.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player chooser = sa.getActivatingPlayer();
        final Game game = chooser.getGame();

        for (final Card tgt : getTargetCards(sa)) {
            final Card c = game.getCardState(tgt);
            if (!c.isInPlay() && !c.isInZone(ZoneType.Stack)) {
                continue;
            }
            final List<NumberNudge> options = NumberInstances.of(c);
            if (options.isEmpty()) {
                continue;
            }
            final List<String> labels = Lists.newArrayList();
            for (final NumberNudge n : options) {
                labels.add(n.label);
            }
            final String picked = chooser.getController().chooseStringForEffect(labels, sa,
                    "Choose a number on " + c.getName() + " to change");
            if (picked == null || !labels.contains(picked)) {
                continue;
            }
            final NumberNudge base = options.get(labels.indexOf(picked));
            final String up = ADD + " (" + base.from + " → " + (base.from + 1) + ")";
            final String down = SUBTRACT + " (" + base.from + " → " + (base.from - 1) + ")";
            final String dir = chooser.getController().chooseStringForEffect(Lists.newArrayList(up, down), sa,
                    picked + ": add or subtract 1?");
            final int to = down.equals(dir) ? base.from - 1 : base.from + 1;

            final long ts = game.getNextTimestamp();
            c.addNumberNudge(base.changedTo(to, ts));
            game.getAction().checkStaticAbilities();
            game.getEndOfTurn().addUntil(new GameCommand() {
                private static final long serialVersionUID = 1L;
                @Override
                public void run() {
                    c.removeNumberNudge(ts);
                }
            });
            game.getAction().notifyOfValue(sa, c, picked + " now reads " + to + " until end of turn.", null);
        }
    }
}
