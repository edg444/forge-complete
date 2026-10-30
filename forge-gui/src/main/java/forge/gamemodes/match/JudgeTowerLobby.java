package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.List;

import forge.game.judgetower.JudgeTowerOptions;
import forge.gui.util.SGuiChoose;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

/**
 * Judge Tower's house rules, asked as the match starts and remembered (FPref.JUDGE_TOWER_RULES). A menu of the rules
 * with their current values, which needs no screen of its own on either client.
 */
public final class JudgeTowerLobby {
    private JudgeTowerLobby() { }

    private static String msg(final String key) {
        return Localizer.getInstance().getMessage(key);
    }

    private static String onOff(final boolean on) {
        return msg(on ? "lblJudgeTowerOn" : "lblOff");
    }

    /** False if the player backed out, which cancels starting the match. */
    public static boolean chooseRules() {
        final JudgeTowerOptions o = JudgeTowerOptions.parse(FModel.getPreferences().getPref(FPref.JUDGE_TOWER_RULES));
        while (true) {
            final List<String> rows = new ArrayList<>();
            final String start = msg("lblJudgeTowerStart");
            rows.add(start);
            final String ownership = msg("lblJudgeTowerOwnership") + ": " + label("lblJudgeTowerOwnership", o.ownership);
            final String life = msg("lblJudgeTowerLife") + ": " + label("lblJudgeTowerLife", o.life);
            final String hidden = msg("lblJudgeTowerHidden") + ": " + label("lblJudgeTowerHidden", o.hidden);
            final String points = msg("lblJudgeTowerPoints") + ": " + o.pointsToWin;
            final String x = msg("lblJudgeTowerX") + ": " + o.xValue;
            final String hand = msg("lblJudgeTowerHand") + ": " + o.startingHand;
            final String graveyard = msg("lblJudgeTowerGraveyard") + ": " + msg(o.sharedGraveyard ? "lblJudgeTowerGraveyardShared" : "lblJudgeTowerGraveyardSeparate");
            final String strikes = msg("lblJudgeTowerStrikes") + ": " + o.strikes;
            final String oneAndDone = msg("lblJudgeTowerOneAndDone") + ": " + onOff(o.oneAndDone);
            rows.addAll(List.of(ownership, life, hidden, points, x, hand, graveyard, strikes, oneAndDone));

            final String row = SGuiChoose.oneOrNone(msg("lblJudgeTowerRules"), rows, start, null);
            if (row == null) {
                return false;
            }
            if (row.equals(start)) {
                FModel.getPreferences().setPref(FPref.JUDGE_TOWER_RULES, o.serialize());
                FModel.getPreferences().save();
                return true;
            }
            if (row.equals(ownership)) {
                o.ownership = chooseEnum("lblJudgeTowerOwnership", JudgeTowerOptions.Ownership.values(), o.ownership);
            } else if (row.equals(life)) {
                o.life = chooseEnum("lblJudgeTowerLife", JudgeTowerOptions.LifeRule.values(), o.life);
            } else if (row.equals(hidden)) {
                o.hidden = chooseEnum("lblJudgeTowerHidden", JudgeTowerOptions.HiddenInfo.values(), o.hidden);
            } else if (row.equals(points)) {
                o.pointsToWin = chooseNumber("lblJudgeTowerPoints", 1, 9, o.pointsToWin);
            } else if (row.equals(x)) {
                o.xValue = chooseNumber("lblJudgeTowerX", 0, 9, o.xValue);
            } else if (row.equals(hand)) {
                o.startingHand = chooseNumber("lblJudgeTowerHand", 0, 7, o.startingHand);
            } else if (row.equals(graveyard)) {
                o.sharedGraveyard = !o.sharedGraveyard;
            } else if (row.equals(strikes)) {
                o.strikes = chooseNumber("lblJudgeTowerStrikes", 1, 5, o.strikes);
            } else if (row.equals(oneAndDone)) {
                o.oneAndDone = !o.oneAndDone;
            }
        }
    }

    private static String label(final String prefix, final Enum<?> value) {
        return msg(prefix + value.name());
    }

    private static <E extends Enum<E>> E chooseEnum(final String prefix, final E[] values, final E current) {
        final List<String> options = new ArrayList<>();
        for (final E v : values) {
            options.add(label(prefix, v));
        }
        final String chosen = SGuiChoose.oneOrNone(msg(prefix), options, label(prefix, current), null);
        return chosen == null ? current : values[options.indexOf(chosen)];
    }

    private static int chooseNumber(final String key, final int min, final int max, final int current) {
        final List<Integer> options = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            options.add(i);
        }
        final Integer chosen = SGuiChoose.oneOrNone(msg(key), options, current, null);
        return chosen == null ? current : chosen;
    }
}
