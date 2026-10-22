package forge.screens.match;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.toolbox.FSkin;
import forge.view.FDialog;

/**
 * Advanced Tactics: its 5x5 grid as a small window beside the match - each creature by name, power/toughness and
 * controller, yours tinted blue and opponents' red. Pointing at one shows it in the card detail panel.
 */
public final class TacticsGridWindow {
    private static final int SIZE = 5;
    private static final Color MINE = new Color(70, 110, 170);
    private static final Color THEIRS = new Color(160, 70, 70);
    private static final Color EMPTY = new Color(60, 60, 60);

    private final CMatchUI matchUI;
    private final FDialog window = new FDialog(false, true, "4");
    private final JLabel[] cells = new JLabel[SIZE * SIZE];
    private final CardView[] shown = new CardView[SIZE * SIZE];

    public TacticsGridWindow(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        window.setTitle("Advanced Tactics");
        final JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 3, 3));
        grid.setOpaque(false);
        for (int i = 0; i < cells.length; i++) {
            final int index = i;
            final JLabel cell = new JLabel("", SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setForeground(Color.WHITE);
            cell.setFont(FSkin.getFont(11).getBaseFont());
            cell.setPreferredSize(new Dimension(116, 64));
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (shown[index] != null) {
                        matchUI.setCard(shown[index]);
                    }
                }
            });
            cells[i] = cell;
            grid.add(cell);
        }
        window.add(grid, "grow");
        window.pack();
        window.setCursor(Cursor.getDefaultCursor());
    }

    /** Shows the host's grid as it now is; hides the window once the grid is gone. */
    public void update(final CardView host) {
        final String data = host == null ? "" : host.getTacticsGrid();
        if (data.isEmpty() || host.getZone() != ZoneType.Battlefield) {
            window.setVisible(false);
            return;
        }
        final Map<Integer, CardView> onBattlefield = new HashMap<>();
        for (final PlayerView p : matchUI.getGameView().getPlayers()) {
            for (final CardView c : p.getCards(ZoneType.Battlefield)) {
                onBattlefield.put(c.getId(), c);
            }
        }
        final String[] ids = data.split(",");
        for (int i = 0; i < cells.length; i++) {
            final CardView c = i < ids.length ? onBattlefield.get(Integer.parseInt(ids[i].trim())) : null;
            shown[i] = c;
            if (c == null) {
                cells[i].setText("");
                cells[i].setBackground(EMPTY);
                continue;
            }
            final PlayerView controller = c.getController();
            final String pt = c.getCurrentState().getPower() + "/" + c.getCurrentState().getToughness();
            cells[i].setText("<html><center>" + c.getName() + "<br>" + pt + "<br><i>"
                    + (controller == null ? "" : controller.getName()) + "</i></center></html>");
            cells[i].setBackground(controller != null && matchUI.isLocalPlayer(controller) ? MINE : THEIRS);
        }
        if (!window.isVisible()) {
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        }
        window.repaint();
    }

    public void close() {
        window.setVisible(false);
        window.dispose();
    }
}
