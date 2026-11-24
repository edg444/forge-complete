package forge.screens.home.conquest;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gui.framework.EDocID;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;

/** Travel to another unlocked plane, or spend planeswalk emblems to unlock a new one. */
public final class VSubmenuConquestPlaneswalk extends AConquestSubmenu {
    public static final VSubmenuConquestPlaneswalk INSTANCE = new VSubmenuConquestPlaneswalk();

    private final ConquestPlaneSelector planeSelector = new ConquestPlaneSelector() {
        @Override
        protected void onSelectionChange() {
            updateButton();
        }
    };
    private final FButton btnPlaneswalk = new FButton();
    private final FLabel lblEmblems = new FLabel.Builder().fontSize(15).fontAlign(SwingConstants.CENTER).build();
    private final FLabel lblHint = new FLabel.Builder().fontSize(12).fontAlign(SwingConstants.CENTER)
            .text(localizer.getMessage("lblClickArtForPlaneInfo")).build();
    private int unlockCost;

    private VSubmenuConquestPlaneswalk() {
        super("lblPlaneswalk", true);
        lblEmblems.setIcon(ConquestSkin.getIcon(FSkinProp.IMG_PW_BADGE_COMMON, 24));
        btnPlaneswalk.setCommand(this::planeswalk);
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTPLANESWALK;
    }

    @Override
    protected void populateContent(final JPanel display) {
        display.add(planeSelector, "w 98%!, growy, pushy, gap 1% 0 0 6px");
        display.add(lblHint, "w 98%!, h 20px!, gap 1% 0 0 6px");
        display.add(lblEmblems, "w 98%!, h 28px!, gap 1% 0 0 6px");
        display.add(btnPlaneswalk, "w 360px!, h 40px!, ax center, gapbottom 15px");
        final ConquestData model = ConquestDesktop.model();
        planeSelector.updateReachablePlanes();
        planeSelector.setCurrentPlane(model.getCurrentPlane());
        planeSelector.activate();
        updateButton();
    }

    private void updateButton() {
        final ConquestData model = ConquestDesktop.model();
        final ConquestPlane plane = planeSelector.getSelectedPlane();
        if (model == null || plane == null) {
            btnPlaneswalk.setEnabled(false);
            return;
        }
        final int emblems = model.getPlaneswalkEmblems();
        lblEmblems.setText(localizer.getMessage("lblPlaneswalkEmblems") + ": " + emblems);
        if (model.isPlaneUnlocked(plane)) {
            unlockCost = 0;
            btnPlaneswalk.setText(localizer.getMessage("lblPlaneswalk"));
            btnPlaneswalk.setEnabled(true);
            lblEmblems.setForeground(ConquestDesktop.TEXT_COLOR);
        } else {
            final ConquestPreferences prefs = FModel.getConquestPreferences();
            unlockCost = prefs.getPrefInt(CQPref.PLANESWALK_FIRST_UNLOCK)
                    + prefs.getPrefInt(CQPref.PLANESWALK_UNLOCK_INCREASE) * (model.getUnlockedPlaneCount() - 1);
            btnPlaneswalk.setText(localizer.getMessage("lblUnlockPlaneForEmblems", emblems, unlockCost));
            btnPlaneswalk.setEnabled(emblems >= unlockCost);
            lblEmblems.setForeground(emblems >= unlockCost ? ConquestDesktop.TEXT_COLOR : Color.RED);
        }
    }

    private void planeswalk() {
        final ConquestData model = ConquestDesktop.model();
        final ConquestPlane plane = planeSelector.getSelectedPlane();
        if (model == null || plane == null) {
            return;
        }
        if (model.isPlaneUnlocked(plane)) {
            model.planeswalkTo(plane);
            model.saveData();
            planeSelector.deactivate();
            ConquestDesktop.open(EDocID.HOME_CONQUESTMULTIVERSE);
        } else if (model.spendPlaneswalkEmblems(unlockCost)) {
            model.unlockPlane(plane);
            model.saveData();
            updateButton();
        }
    }
}
