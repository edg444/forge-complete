package forge.screens.home.conquest;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.framework.EDocID;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.screens.home.conquest.ConquestDesktop.Named;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.FileUtil;
import net.miginfocom.swing.MigLayout;

/** Lists saved conquests to continue, rename or delete. */
public final class VSubmenuConquestLoad extends AConquestSubmenu {
    public static final VSubmenuConquestLoad INSTANCE = new VSubmenuConquestLoad();

    private final DefaultListModel<Named<ConquestData>> listModel = new DefaultListModel<>();
    private final FList<Named<ConquestData>> lstConquests = new FList<>(listModel);
    private final FLabel lblInfo = new FLabel.Builder().fontSize(12).fontAlign(SwingConstants.CENTER).build();
    private final FButton btnLoad = new FButton(localizer.getMessage("lblLoad"));
    private final FButton btnNew = new FButton(localizer.getMessage("lblNewConquest"));
    private final FButton btnRename = new FButton(localizer.getMessage("lblRename"));
    private final FButton btnDelete = new FButton(localizer.getMessage("lblDelete"));

    private VSubmenuConquestLoad() {
        super("lblLoadConquest", false);
        lstConquests.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstConquests.setFont(forge.toolbox.FSkin.getRelativeFont(14).getBaseFont());
        lstConquests.addListSelectionListener(e -> updateButtons());
        lstConquests.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelected();
                }
            }
        });
        btnLoad.setCommand(this::loadSelected);
        btnNew.setCommand(() -> ConquestDesktop.open(EDocID.HOME_CONQUESTNEW));
        btnRename.setCommand(this::renameSelected);
        btnDelete.setCommand(this::deleteSelected);
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTLOAD;
    }

    @Override
    protected void populateContent(final JPanel display) {
        lblInfo.setText(localizer.getMessage("lblOldConquestDataPutIntoDirAndRestartForge", ForgeConstants.CONQUEST_SAVE_DIR));
        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 10, ax center"));
        buttons.setOpaque(false);
        buttons.add(btnLoad, "w 150px!, h 36px!");
        buttons.add(btnNew, "w 150px!, h 36px!");
        buttons.add(btnRename, "w 150px!, h 36px!");
        buttons.add(btnDelete, "w 150px!, h 36px!");
        display.add(lblInfo, "w 98%!, h 24px!, gap 1% 0 0 6px");
        display.add(new FScrollPane(lstConquests, true), "w 98%!, growy, pushy, gap 1% 0 0 10px");
        display.add(buttons, "w 98%!, gap 1% 0 0 15px");
        refresh();
    }

    private void refresh() {
        listModel.clear();
        final List<ConquestData> conquests = new ArrayList<>();
        final File[] dirs = new File(ForgeConstants.CONQUEST_SAVE_DIR).listFiles();
        if (dirs != null) {
            for (final File f : dirs) {
                if (f.isDirectory()) {
                    final ConquestData data = new ConquestData(f);
                    if (data.getCurrentLocation() != null && data.getPlaneswalker() != null) {
                        conquests.add(data);
                    }
                }
            }
        }
        conquests.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
        final String current = FModel.getConquestPreferences().getPref(CQPref.CURRENT_CONQUEST);
        int selected = conquests.isEmpty() ? -1 : 0;
        for (final ConquestData data : conquests) {
            if (data.getName().equals(current)) {
                selected = listModel.size();
            }
            listModel.addElement(new Named<>(data, describe(data)));
        }
        if (selected >= 0) {
            lstConquests.setSelectedIndex(selected);
            lstConquests.ensureIndexIsVisible(selected);
        }
        updateButtons();
    }

    private String describe(final ConquestData data) {
        return data.getName() + "   (" + data.getProgress() + ")   "
                + data.getPlaneswalker().getDisplayName() + " - " + data.getCurrentPlane().getName().replace("_", " ")
                + "   |   " + localizer.getMessage("lblConquestUnlockedCards", data.getUnlockedCardCount())
                + "   |   " + localizer.getMessage("lblConquestShards", data.getAEtherShards());
    }

    private ConquestData getSelected() {
        final Named<ConquestData> sel = lstConquests.getSelectedValue();
        return sel == null ? null : sel.value();
    }

    private void updateButtons() {
        final boolean enabled = getSelected() != null;
        btnLoad.setEnabled(enabled);
        btnRename.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
    }

    private void loadSelected() {
        final ConquestData conquest = getSelected();
        if (conquest == null) {
            return;
        }
        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquest.getName());
        FModel.getConquestPreferences().save();
        FModel.getConquest().setModel(conquest);
        ConquestDesktop.open(EDocID.HOME_CONQUESTMULTIVERSE);
    }

    private void renameSelected() {
        final ConquestData conquest = getSelected();
        if (conquest == null) {
            return;
        }
        final String oldName = conquest.getName();
        String newName;
        while (true) {
            newName = SOptionPane.showInputDialog(localizer.getMessage("lblEnterNewConquestName"),
                    localizer.getMessage("lblRenameConquest"), null, oldName);
            if (newName == null) {
                return;
            }
            newName = QuestUtil.cleanString(newName);
            if (newName.equals(oldName)) {
                return;
            }
            if (newName.isEmpty()) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblPleaseSpecifyConquestName"));
                continue;
            }
            boolean exists = false;
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).value().getName().equalsIgnoreCase(newName)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblConquestNameExistsPleasePickAnotherName"));
                continue;
            }
            break;
        }
        final ConquestPreferences prefs = FModel.getConquestPreferences();
        if (oldName.equals(prefs.getPref(CQPref.CURRENT_CONQUEST))) {
            prefs.setPref(CQPref.CURRENT_CONQUEST, newName);
            prefs.save();
        }
        // the active conquest is a separate copy whose folder and deck storage would point at the old name
        final ConquestData active = FModel.getConquest().getModel();
        if (active != null && active.getName().equals(oldName)) {
            FModel.getConquest().setModel(null);
        }
        conquest.rename(newName);
        refresh();
    }

    private void deleteSelected() {
        final ConquestData conquest = getSelected();
        if (conquest == null) {
            return;
        }
        if (!FOptionPane.showConfirmDialog(localizer.getMessage("lblAreYouSuerDeleteConquest", conquest.getName()),
                localizer.getMessage("lblDeleteConquest"), localizer.getMessage("lblDelete"), localizer.getMessage("lblCancel"))) {
            return;
        }
        final ConquestData active = FModel.getConquest().getModel();
        if (active != null && active.getName().equals(conquest.getName())) {
            FModel.getConquest().setModel(null);
        }
        FileUtil.deleteDirectory(conquest.getDirectory());
        refresh();
    }
}
