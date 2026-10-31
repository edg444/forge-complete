package forge.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.judgetower.table.TableLink;
import forge.game.judgetower.table.TableSeat;
import forge.game.spellability.SpellAbility;
import forge.gamemodes.match.input.InputTable;
import forge.trackable.TrackableCollection;
import forge.util.Localizer;

/** A human on screen at Judge Tower's table. With more than one at a device, the screen goes to whoever is asked. */
public final class HumanTableSeat implements TableSeat {
    private final PlayerControllerHuman controller;
    private volatile boolean autoOk;

    public HumanTableSeat(final PlayerControllerHuman controller0) {
        controller = controller0;
    }

    private TableLink link() {
        return controller.getGame().getJudge().getTableLink();
    }

    private void takeTheScreen() {
        controller.getGui().setCurrentPlayer(controller.getLocalPlayerView());
    }

    private void startAutoOk() {
        autoOk = true;
    }

    @Override
    public void window(final Window kind, final String prompt) {
        // the player's own to carry out is never skipped
        if (autoOk && kind != Window.RESOLVE && kind != Window.ATTACK && kind != Window.BLOCK && kind != Window.DISCARD) {
            return;
        }
        if (controller.getGame().isGameOver()) {
            return;
        }
        takeTheScreen();
        new InputTable(controller, link(), kind, prompt, this::startAutoOk).showAndWait();
    }

    @Override
    public SpellAbility priority(final String prompt) {
        if (autoOk || controller.getGame().isGameOver()) {
            return null;
        }
        takeTheScreen();
        final InputTable input = new InputTable(controller, link(), null, prompt, this::startAutoOk);
        input.showAndWait();
        return input.getPlay();
    }

    @Override
    public boolean appeal(final String reason) {
        takeTheScreen();
        final Localizer loc = Localizer.getInstance();
        return controller.getGui().confirm(null, reason, false, ImmutableList.of(loc.getMessage("lblJudgeOverrule"), loc.getMessage("lblJudgeAcceptRuling")));
    }

    /** A choice with what it says, so two the same are still two. */
    private record Option(int index, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    public List<Integer> choose(final String prompt, final List<String> options, final int min, final int max) {
        takeTheScreen();
        final List<Option> rows = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            rows.add(new Option(i, options.get(i)));
        }
        final List<Option> chosen = controller.getGui().many(prompt, null, min, max, rows, null);
        final List<Integer> result = new ArrayList<>();
        if (chosen != null) {
            chosen.forEach(o -> result.add(o.index()));
        }
        return result;
    }

    @Override
    public Map<Card, Integer> assignDamage(final Card attacker, final List<Card> blockers, final int damage, final boolean toDefender) {
        takeTheScreen();
        final TrackableCollection<CardView> views = new TrackableCollection<>();
        final Map<CardView, Card> back = new HashMap<>();
        for (final Card b : blockers) {
            views.add(b.getView());
            back.put(b.getView(), b);
        }
        final Map<CardView, Integer> assigned = controller.getGui().assignCombatDamage(attacker.getView(), views, damage,
                toDefender ? attacker.getController().getOpponents().getFirst().getView() : null, false, false);
        final Map<Card, Integer> result = new HashMap<>();
        if (assigned != null) {
            assigned.forEach((view, amount) -> result.put(view == null || view.getId() == -1 ? null : back.get(view), amount));
        }
        return result;
    }

    @Override
    public void stopAutoOk() {
        autoOk = false;
    }
}
