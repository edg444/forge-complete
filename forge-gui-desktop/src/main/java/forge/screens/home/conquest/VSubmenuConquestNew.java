package forge.screens.home.conquest;

import java.awt.CardLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import com.google.common.collect.Iterables;

import forge.gamemodes.planarconquest.ConquestController;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.localinstance.achievements.PlaneswalkerAchievements;
import forge.model.FModel;
import forge.screens.home.conquest.ConquestDesktop.Named;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import net.miginfocom.swing.MigLayout;

/** New conquest wizard: starting plane, then commander, then planeswalker, then a name. */
public final class VSubmenuConquestNew extends AConquestSubmenu {
    public static final VSubmenuConquestNew INSTANCE = new VSubmenuConquestNew();

    private static final String[] STEP_KEYS = {"lblSelectStartingPlane", "lblSelectStartingCommander", "lblSelectStartingPlaneswalker"};

    private final JPanel pnlSteps = new JPanel(new CardLayout());
    private final FLabel lblStep = new FLabel.Builder().fontSize(16).fontAlign(SwingConstants.CENTER).build();
    private final FLabel lblHint = new FLabel.Builder().fontSize(12).fontAlign(SwingConstants.CENTER).build();
    private final FButton btnBack = new FButton(localizer.getMessage("lblBack"));
    private final FButton btnNext = new FButton(localizer.getMessage("lblNext"));
    private final ConquestPlaneSelector planeSelector = new ConquestPlaneSelector();
    private final DefaultListModel<Named<PaperCard>> commanderModel = new DefaultListModel<>();
    private final FList<Named<PaperCard>> lstCommanders = new FList<>(commanderModel);
    private final ConquestDesktop.CardPreview commanderPreview = new ConquestDesktop.CardPreview();
    private final DefaultListModel<Named<PaperCard>> planeswalkerModel = new DefaultListModel<>();
    private final FList<Named<PaperCard>> lstPlaneswalkers = new FList<>(planeswalkerModel);
    private final ConquestDesktop.CardPreview planeswalkerPreview = new ConquestDesktop.CardPreview();
    private int step;
    private ConquestPlane startingPlane;
    private PaperCard startingCommander;

    private VSubmenuConquestNew() {
        super("lblNewConquest", false);
        pnlSteps.setOpaque(false);
        pnlSteps.add(planeSelector, "0");
        pnlSteps.add(buildListStep(lstCommanders, commanderPreview), "1");
        pnlSteps.add(buildListStep(lstPlaneswalkers, planeswalkerPreview), "2");

        lstCommanders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCommanders.addListSelectionListener(e -> {
            final Named<PaperCard> sel = lstCommanders.getSelectedValue();
            commanderPreview.setCard(sel == null ? null : sel.value());
        });
        lstPlaneswalkers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstPlaneswalkers.addListSelectionListener(e -> {
            final Named<PaperCard> sel = lstPlaneswalkers.getSelectedValue();
            planeswalkerPreview.setImage(sel == null ? null : ConquestDesktop.toImage(
                    PlaneswalkerAchievements.getTrophyImage(sel.value().getName(), sel.value())));
        });

        btnBack.setCommand(() -> showStep(step - 1));
        btnNext.setCommand(this::advance);
    }

    private static JPanel buildListStep(final FList<Named<PaperCard>> list, final ConquestDesktop.CardPreview preview) {
        final JPanel panel = new JPanel(new MigLayout("insets 0, gap 10, fill"));
        panel.setOpaque(false);
        panel.add(new FScrollPane(list, true), "w 40%!, growy, pushy");
        panel.add(preview, "grow, push");
        return panel;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTNEW;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 10, ax center"));
        buttons.setOpaque(false);
        buttons.add(btnBack, "w 160px!, h 36px!");
        buttons.add(btnNext, "w 160px!, h 36px!");
        display.add(lblStep, "w 98%!, h 26px!, gap 1% 0 0 0");
        display.add(lblHint, "w 98%!, h 20px!, gap 1% 0 0 6px");
        display.add(pnlSteps, "w 98%!, growy, pushy, gap 1% 0 0 10px");
        display.add(buttons, "w 98%!, gap 1% 0 0 15px");
        planeSelector.updateReachablePlanes();
        planeSelector.reset();
        showStep(0);
    }

    private void showStep(final int step0) {
        if (step0 < 0) {
            return;
        }
        step = step0;
        ((CardLayout) pnlSteps.getLayout()).show(pnlSteps, String.valueOf(step));
        lblStep.setText(localizer.getMessage(STEP_KEYS[step]).split("\n")[0]);
        lblHint.setText(step == 0 ? localizer.getMessage("lblClickArtForPlaneInfo") : "");
        btnBack.setEnabled(step > 0);
        btnNext.setText(localizer.getMessage(step == STEP_KEYS.length - 1 ? "lblStart" : "lblNext"));
        if (step == 0) {
            planeSelector.activate();
        } else {
            planeSelector.deactivate();
        }
    }

    private void advance() {
        switch (step) {
            case 0 -> {
                startingPlane = planeSelector.getSelectedPlane();
                fill(commanderModel, lstCommanders, startingPlane.getCommanders());
                showStep(1);
            }
            case 1 -> {
                final Named<PaperCard> sel = lstCommanders.getSelectedValue();
                if (sel == null) {
                    return;
                }
                startingCommander = sel.value();
                fill(planeswalkerModel, lstPlaneswalkers, ConquestUtil.getStartingPlaneswalkerOptions(startingCommander));
                showStep(2);
            }
            default -> {
                final Named<PaperCard> sel = lstPlaneswalkers.getSelectedValue();
                if (sel != null) {
                    startConquest(sel.value());
                }
            }
        }
    }

    private static void fill(final DefaultListModel<Named<PaperCard>> model, final FList<Named<PaperCard>> list, final Iterable<PaperCard> cards) {
        model.clear();
        final List<PaperCard> list0 = new java.util.ArrayList<>();
        cards.forEach(list0::add);
        for (final PaperCard card : list0) {
            model.addElement(new Named<>(card, card.getDisplayName()));
        }
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
            list.ensureIndexIsVisible(0);
        }
    }

    private void startConquest(final PaperCard planeswalker) {
        final String name = ConquestUtil.promptForName();
        if (name == null) {
            return;
        }
        planeSelector.deactivate();
        final ConquestController qc = FModel.getConquest();
        qc.setModel(new ConquestData(name, startingPlane, planeswalker, startingCommander));
        qc.getDecks().add(Iterables.getFirst(qc.getModel().getCommanders(), null).getDeck());
        qc.getModel().saveData();
        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, name);
        FModel.getConquestPreferences().save();
        ConquestDesktop.open(EDocID.HOME_CONQUESTMULTIVERSE);
    }
}
