package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.event.GameEventTacticsGrid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * Advanced Tactics' 5x5 grid, kept on the host (Card.getTacticsGrid: 25 card ids by row, 0 for an empty space; a
 * creature that left the battlefield leaves its space empty). Mode$ Create: starting with the top left space and
 * skipping every other space, creatures chosen at random fill it (then the skipped spaces, if there are more than 13).
 * Mode$ Place: the Defined$ creature that just entered goes into an empty space its controller picks (an AI's is
 * random). Mode$ Move: the targeted creature moves one space left, right, up, or down, into an empty space.
 * Mode$ Remove: the grid is gone, as the host leaves the battlefield.
 */
public class TacticsGridEffect extends SpellAbilityEffect {

    public static final int SIZE = 5;

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return switch (sa.getParamOrDefault("Mode", "Create")) {
            case "Place" -> "Put the creature in an empty space of the grid.";
            case "Move" -> "Move " + getTargetCards(sa) + " one space.";
            default -> "Create a 5x5 grid.";
        };
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        switch (sa.getParamOrDefault("Mode", "Create")) {
            case "Place" -> {
                if (host.getTacticsGrid() == null) {
                    return;
                }
                for (final Card c : getDefinedCardsOrTargeted(sa)) {
                    place(sa, host, game.getCardState(c));
                }
            }
            case "Remove" -> host.setTacticsGrid(null);
            case "Move" -> {
                if (host.getTacticsGrid() == null) {
                    return;
                }
                for (final Card c : getTargetCards(sa)) {
                    move(sa, host, game.getCardState(c));
                }
            }
            default -> create(host);
        }
        game.fireEvent(new GameEventTacticsGrid(host.getView()));
    }

    private static void create(final Card host) {
        final Game game = host.getGame();
        final int[] grid = new int[SIZE * SIZE];
        final List<Card> creatures = new ArrayList<>(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Card::isCreature));
        Collections.shuffle(creatures, MyRandom.getRandom());
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < grid.length; i += 2) {
            order.add(i);
        }
        for (int i = 1; i < grid.length; i += 2) {
            order.add(i);
        }
        for (int i = 0; i < creatures.size() && i < order.size(); i++) {
            grid[order.get(i)] = creatures.get(i).getId();
        }
        host.setTacticsGrid(grid);
        game.getGameLog().add(GameLogEntryType.INFORMATION, host + " creates a 5x5 grid.");
    }

    private static void place(final SpellAbility sa, final Card host, final Card c) {
        if (c == null || !c.isInPlay()) {
            return;
        }
        final int[] grid = host.getTacticsGrid();
        final int old = cellOf(host, c);
        if (old >= 0) {
            grid[old] = 0;
        }
        final List<Integer> empty = emptyCells(host);
        if (empty.isEmpty()) {
            host.setTacticsGrid(grid);
            return;
        }
        final Player p = c.getController();
        int cell;
        if (p.getController().isAI() || empty.size() == 1) {
            cell = Aggregates.random(empty);
        } else {
            final Map<String, Integer> byLabel = new LinkedHashMap<>();
            for (final int i : empty) {
                byLabel.put(label(i), i);
            }
            final String pick = p.getController().chooseSomeType("empty space for " + c.getName(), sa, new ArrayList<>(byLabel.keySet()));
            cell = pick == null ? empty.get(0) : byLabel.get(pick);
        }
        grid[cell] = c.getId();
        host.setTacticsGrid(grid);
        host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, c + " goes into the grid at " + label(cell) + ".");
    }

    private static void move(final SpellAbility sa, final Card host, final Card c) {
        if (c == null || !c.isInPlay()) {
            return;
        }
        final int from = cellOf(host, c);
        if (from < 0) {
            return;
        }
        final int row = from / SIZE;
        final int col = from % SIZE;
        final Map<String, Integer> moves = new LinkedHashMap<>();
        final List<Integer> empty = emptyCells(host);
        addMove(moves, empty, "Up", row - 1, col);
        addMove(moves, empty, "Down", row + 1, col);
        addMove(moves, empty, "Left", row, col - 1);
        addMove(moves, empty, "Right", row, col + 1);
        if (moves.isEmpty()) {
            return;
        }
        final Player p = sa.getActivatingPlayer();
        final String pick = p.getController().isAI() ? Aggregates.random(new ArrayList<>(moves.keySet()))
                : p.getController().chooseSomeType("direction to move " + c.getName(), sa, new ArrayList<>(moves.keySet()));
        if (pick == null) {
            return;
        }
        final int[] grid = host.getTacticsGrid();
        grid[from] = 0;
        grid[moves.get(pick)] = c.getId();
        host.setTacticsGrid(grid);
        host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, c + " moves to " + label(moves.get(pick)) + ".");
    }

    private static void addMove(final Map<String, Integer> moves, final List<Integer> empty, final String dir, final int row, final int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return;
        }
        final int cell = row * SIZE + col;
        if (empty.contains(cell)) {
            moves.put(dir + " (" + label(cell) + ")", cell);
        }
    }

    public static String label(final int cell) {
        return "row " + (cell / SIZE + 1) + ", column " + (cell % SIZE + 1);
    }

    /** The creature's space in the host's grid, or -1. */
    public static int cellOf(final Card host, final Card c) {
        final int[] grid = host.getTacticsGrid();
        if (grid == null || c == null) {
            return -1;
        }
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == c.getId() && occupied(host.getGame(), grid[i])) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> emptyCells(final Card host) {
        final List<Integer> empty = new ArrayList<>();
        final int[] grid = host.getTacticsGrid();
        for (int i = 0; grid != null && i < grid.length; i++) {
            if (!occupied(host.getGame(), grid[i])) {
                empty.add(i);
            }
        }
        return empty;
    }

    private static boolean occupied(final Game game, final int id) {
        if (id == 0) {
            return false;
        }
        final Card c = game.findById(id);
        return c != null && c.isInPlay();
    }

    /** Whether the two creatures are in orthogonally adjacent spaces of the host's grid. */
    public static boolean adjacent(final Card host, final Card a, final Card b) {
        final int x = cellOf(host, a);
        final int y = cellOf(host, b);
        if (x < 0 || y < 0) {
            return false;
        }
        return Math.abs(x / SIZE - y / SIZE) + Math.abs(x % SIZE - y % SIZE) == 1;
    }
}
