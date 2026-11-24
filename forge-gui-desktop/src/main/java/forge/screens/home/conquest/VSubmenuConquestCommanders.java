package forge.screens.home.conquest;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import forge.deck.DeckFormat;
import forge.deckchooser.FDeckViewer;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestRecord;
import forge.gui.framework.EDocID;
import forge.screens.home.conquest.ConquestDesktop.Named;
import forge.toolbox.FButton;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import net.miginfocom.swing.MigLayout;

/** Choose which commander leads the next battle, and view or edit each commander's deck. */
public final class VSubmenuConquestCommanders extends AConquestSubmenu {
    public static final VSubmenuConquestCommanders INSTANCE = new VSubmenuConquestCommanders();

    private final DefaultListModel<Named<ConquestCommander>> listModel = new DefaultListModel<>();
    private final FList<Named<ConquestCommander>> lstCommanders = new FList<>(listModel);
    private final ConquestDesktop.CardPreview preview = new ConquestDesktop.CardPreview();
    private final FButton btnSelect = new FButton(localizer.getMessage("lblSelectCommander"));
    private final FButton btnView = new FButton(localizer.getMessage("lblViewDeck"));
    private final FButton btnEdit = new FButton(localizer.getMessage("btnEditDeck"));

    private VSubmenuConquestCommanders() {
        super("lblCommanders", true);
        lstCommanders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCommanders.setFont(FSkin.getRelativeFont(14).getBaseFont());
        lstCommanders.addListSelectionListener(e -> {
            final ConquestCommander commander = getSelected();
            preview.setCard(commander == null ? null : commander.getCard());
            updateButtons();
        });
        lstCommanders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectCommander();
                }
            }
        });
        btnSelect.setCommand(this::selectCommander);
        btnView.setCommand(() -> {
            final ConquestCommander commander = getSelected();
            if (commander != null) {
                FDeckViewer.show(commander.getDeck());
            }
        });
        btnEdit.setCommand(() -> {
            final ConquestCommander commander = getSelected();
            if (commander != null) {
                ConquestDesktop.editDeck(commander);
            }
        });
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTCOMMANDERS;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final JPanel content = new JPanel(new MigLayout("insets 0, gap 10"));
        content.setOpaque(false);
        content.add(new FScrollPane(lstCommanders, true), "w 55%!, growy, pushy");
        content.add(preview, "grow, push");
        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 10, ax center"));
        buttons.setOpaque(false);
        buttons.add(btnSelect, "w 180px!, h 36px!");
        buttons.add(btnView, "w 180px!, h 36px!");
        buttons.add(btnEdit, "w 180px!, h 36px!");
        display.add(content, "w 98%!, growy, pushy, gap 1% 0 0 10px");
        display.add(buttons, "w 98%!, gap 1% 0 0 15px");
        refresh(null);
    }

    private void refresh(final ConquestCommander toSelect) {
        final ConquestData model = ConquestDesktop.model();
        final ConquestCommander current = model.getSelectedCommander();
        final ConquestCommander target = toSelect != null ? toSelect : current;
        listModel.clear();
        int index = 0;
        for (final ConquestCommander commander : model.getCommanders()) {
            if (commander == target) {
                index = listModel.size();
            }
            listModel.addElement(new Named<>(commander, describe(commander, commander == current)));
        }
        if (!listModel.isEmpty()) {
            lstCommanders.setSelectedIndex(index);
            lstCommanders.ensureIndexIsVisible(index);
        }
        updateButtons();
    }

    private String describe(final ConquestCommander commander, final boolean current) {
        final ConquestRecord record = commander.getRecord();
        return (current ? "★ " : "    ") + commander.getDisplayName() + "   —   " + commander.getOrigin()
                + " (" + record.getWins() + "W / " + record.getLosses() + "L)";
    }

    private ConquestCommander getSelected() {
        final Named<ConquestCommander> sel = lstCommanders.getSelectedValue();
        return sel == null ? null : sel.value();
    }

    private void updateButtons() {
        final ConquestCommander commander = getSelected();
        btnSelect.setEnabled(commander != null && commander != ConquestDesktop.model().getSelectedCommander());
        btnView.setEnabled(commander != null);
        btnEdit.setEnabled(commander != null);
    }

    private void selectCommander() {
        final ConquestCommander commander = getSelected();
        if (commander == null) {
            return;
        }
        final String problem = DeckFormat.PlanarConquest.getDeckConformanceProblem(commander.getDeck());
        if (problem != null) {
            FOptionPane.showMessageDialog(localizer.getMessage("lblCantSelectDeckBecause", commander.getName(), problem),
                    localizer.getMessage("lblInvalidDeck"), FOptionPane.INFORMATION_ICON);
            return;
        }
        final ConquestData model = ConquestDesktop.model();
        if (model.getSelectedCommander() != commander) {
            model.setSelectedCommander(commander);
            model.saveData();
        }
        refresh(commander);
    }
}
