package forge.screens.match.controllers;

import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VTacticsGrid;

/**
 * Advanced Tactics: puts the "Grid" tab in the Stack tab's cell when a grid is created (selected once, so it's
 * noticed), keeps it up to date, and takes it out once the grid is gone.
 */
public class CTacticsGrid implements ICDoc {

    private final VTacticsGrid view;
    private CardView host;

    public CTacticsGrid(final CMatchUI matchUI) {
        view = new VTacticsGrid(this, matchUI);
    }

    public VTacticsGrid getView() {
        return view;
    }

    /** The host's grid changed (or is gone). Call on the event dispatch thread. */
    public void show(final CardView host0) {
        host = host0;
        final boolean hasGrid = host != null && !host.getTacticsGrid().isEmpty() && host.getZone() == ZoneType.Battlefield;
        final DragCell cell = view.getParentCell();
        if (!hasGrid) {
            if (cell != null) {
                cell.removeDoc(view);
                view.setParentCell(null);
            }
            return;
        }
        if (cell == null) {
            DragCell stackCell = null;
            // beside the Stack tab, or whichever of Log and Combat a custom layout still has
            for (final EDocID beside : new EDocID[] {EDocID.REPORT_STACK, EDocID.REPORT_LOG, EDocID.REPORT_COMBAT}) {
                if (stackCell == null && beside.getDoc() != null) {
                    stackCell = beside.getDoc().getParentCell();
                }
            }
            if (stackCell != null) {
                stackCell.addDoc(view);
                stackCell.setSelected(view);
            }
        }
        view.refresh(host);
    }

    /** Called as cards change, so the names and power/toughness stay current. */
    @Override
    public void update() {
        if (view.getParentCell() != null) {
            view.refresh(host);
        }
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }
}
