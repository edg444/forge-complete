package forge.screens.match.views;

import java.awt.Color;
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
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CTacticsGrid;
import forge.toolbox.FSkin;
import net.miginfocom.swing.MigLayout;

/**
 * Advanced Tactics: its 5x5 grid as a "Grid" tab beside Stack, Combat and Log, there only while a grid exists. Each
 * space shows its creature's name, power/toughness and controller, yours tinted blue and opponents' red; pointing at
 * one shows it in the card detail panel.
 */
public class VTacticsGrid implements IVDoc<CTacticsGrid> {
    private static final int SIZE = 5;
    private static final Color MINE = new Color(70, 110, 170);
    private static final Color THEIRS = new Color(160, 70, 70);
    private static final Color EMPTY = new Color(60, 60, 60);

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Grid");
    private final CTacticsGrid controller;
    private final CMatchUI matchUI;
    private final JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2));
    private final JLabel[] cells = new JLabel[SIZE * SIZE];
    private final CardView[] shown = new CardView[SIZE * SIZE];

    public VTacticsGrid(final CTacticsGrid controller, final CMatchUI matchUI) {
        this.controller = controller;
        this.matchUI = matchUI;
        grid.setOpaque(false);
        for (int i = 0; i < cells.length; i++) {
            final int index = i;
            final JLabel cell = new JLabel("", SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setForeground(Color.WHITE);
            cell.setFont(FSkin.getFont(10).getBaseFont());
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
    }

    @Override
    public void populate() {
        parentCell.getBody().removeAll();
        parentCell.getBody().setLayout(new MigLayout("insets 2, gap 0, fill"));
        parentCell.getBody().add(grid, "grow, push");
    }

    /** Shows the host's grid as it now is. */
    public void refresh(final CardView host) {
        final String data = host == null ? "" : host.getTacticsGrid();
        final Map<Integer, CardView> onBattlefield = new HashMap<>();
        for (final PlayerView p : matchUI.getGameView().getPlayers()) {
            for (final CardView c : p.getCards(ZoneType.Battlefield)) {
                onBattlefield.put(c.getId(), c);
            }
        }
        final String[] ids = data.isEmpty() ? new String[0] : data.split(",");
        for (int i = 0; i < cells.length; i++) {
            final CardView c = i < ids.length ? onBattlefield.get(Integer.parseInt(ids[i].trim())) : null;
            shown[i] = c;
            if (c == null) {
                cells[i].setText("");
                cells[i].setToolTipText(null);
                cells[i].setBackground(EMPTY);
                continue;
            }
            final PlayerView owner = c.getController();
            final String pt = c.getCurrentState().getPower() + "/" + c.getCurrentState().getToughness();
            cells[i].setText("<html><center>" + c.getName() + "<br>" + pt + "</center></html>");
            cells[i].setToolTipText(c.getName() + " (" + pt + ")" + (owner == null ? "" : " - " + owner.getName()));
            cells[i].setBackground(owner != null && matchUI.isLocalPlayer(owner) ? MINE : THEIRS);
        }
        grid.revalidate();
        grid.repaint();
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_GRID;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CTacticsGrid getLayoutControl() {
        return controller;
    }
}
