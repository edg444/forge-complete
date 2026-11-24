package forge.screens.home.conquest;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import forge.gamemodes.planarconquest.ConquestData;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.localinstance.achievements.PlaneswalkerAchievements;
import forge.screens.home.conquest.ConquestDesktop.Named;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import net.miginfocom.swing.MigLayout;

/** Pick the planeswalker whose token travels the multiverse; unlocked planeswalkers only. */
public final class VSubmenuConquestPlaneswalkers extends AConquestSubmenu {
    public static final VSubmenuConquestPlaneswalkers INSTANCE = new VSubmenuConquestPlaneswalkers();

    private final DefaultListModel<Named<PaperCard>> listModel = new DefaultListModel<>();
    private final FList<Named<PaperCard>> lstPlaneswalkers = new FList<>(listModel);
    private final ConquestDesktop.CardPreview preview = new ConquestDesktop.CardPreview();
    private boolean refreshing;

    private VSubmenuConquestPlaneswalkers() {
        super("lblPlaneswalkers", true);
        lstPlaneswalkers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstPlaneswalkers.setFont(FSkin.getRelativeFont(14).getBaseFont());
        lstPlaneswalkers.addListSelectionListener(e -> {
            final Named<PaperCard> sel = lstPlaneswalkers.getSelectedValue();
            final PaperCard planeswalker = sel == null ? null : sel.value();
            preview.setImage(planeswalker == null ? null : ConquestDesktop.toImage(
                    PlaneswalkerAchievements.getTrophyImage(planeswalker.getName(), planeswalker)));
            if (planeswalker == null || refreshing || e.getValueIsAdjusting()) {
                return;
            }
            final ConquestData model = ConquestDesktop.model();
            if (model.getPlaneswalker() != planeswalker) {
                model.setPlaneswalker(planeswalker);
                model.saveData();
            }
        });
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTPLANESWALKERS;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final JPanel content = new JPanel(new MigLayout("insets 0, gap 10"));
        content.setOpaque(false);
        content.add(new FScrollPane(lstPlaneswalkers, true), "w 55%!, growy, pushy");
        content.add(preview, "grow, push");
        display.add(content, CONTENT_CONSTRAINTS);

        refreshing = true;
        final ConquestData model = ConquestDesktop.model();
        listModel.clear();
        int index = 0;
        for (final PaperCard planeswalker : model.getSortedPlaneswalkers()) {
            if (planeswalker == model.getPlaneswalker()) {
                index = listModel.size();
            }
            listModel.addElement(new Named<>(planeswalker, planeswalker.getDisplayName()));
        }
        if (!listModel.isEmpty()) {
            lstPlaneswalkers.setSelectedIndex(index);
            lstPlaneswalkers.ensureIndexIsVisible(index);
        }
        refreshing = false;
    }
}
