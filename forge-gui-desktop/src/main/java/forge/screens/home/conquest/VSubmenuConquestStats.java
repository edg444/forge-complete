package forge.screens.home.conquest;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.IVConquestStats;
import forge.gui.framework.EDocID;
import forge.gui.interfaces.IButton;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.home.conquest.ConquestDesktop.Named;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import net.miginfocom.swing.MigLayout;

/** Totals for the whole conquest or a single plane. */
public final class VSubmenuConquestStats extends AConquestSubmenu implements IVConquestStats {
    public static final VSubmenuConquestStats INSTANCE = new VSubmenuConquestStats();

    private final FComboBox<Named<ConquestPlane>> cbPlanes = new FComboBox<>();
    private final FLabel lblAEtherShards = statLabel(FSkinProp.IMG_AETHER_SHARD);
    private final FLabel lblPlaneswalkEmblems = statLabel(FSkinProp.IMG_PW_BADGE_COMMON);
    private final FLabel lblTotalWins = statLabel(FSkinProp.ICO_QUEST_PLUS);
    private final FLabel lblTotalLosses = statLabel(FSkinProp.ICO_QUEST_MINUS);
    private final FLabel lblConqueredEvents = statLabel(FSkinProp.IMG_MULTIVERSE);
    private final FLabel lblUnlockedCards = statLabel(FSkinProp.IMG_SPELLBOOK);
    private final FLabel lblCommanders = statLabel(FSkinProp.IMG_COMMANDER);
    private final FLabel lblPlaneswalkers = statLabel(FSkinProp.IMG_PLANESWALKER);
    private boolean refreshing;

    private static FLabel statLabel(final FSkinProp icon) {
        final FLabel label = new FLabel.Builder().fontSize(15).iconScaleFactor(1).build();
        label.setIcon(ConquestSkin.getIcon(icon, 28));
        return label;
    }

    private VSubmenuConquestStats() {
        super("lblStatistics", true);
        cbPlanes.addActionListener(e -> {
            if (refreshing) {
                return;
            }
            final Named<ConquestPlane> sel = cbPlanes.getSelectedItem();
            final ConquestData model = ConquestDesktop.model();
            if (model != null) {
                model.updateStatLabels(this, sel == null ? null : sel.value());
            }
        });
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTSTATS;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final ConquestData model = ConquestDesktop.model();
        refreshing = true;
        cbPlanes.removeAllItems();
        final List<Named<ConquestPlane>> options = new ArrayList<>();
        options.add(new Named<>(null, localizer.getMessage("lblAllPlanes")));
        Named<ConquestPlane> current = null;
        for (final ConquestPlane plane : FModel.getPlanes()) {
            if (!plane.isUnreachable() || model.getCurrentPlane().equals(plane)) {
                final Named<ConquestPlane> option = new Named<>(plane, plane.getName().replace("_", " "));
                if (plane.equals(model.getCurrentPlane())) {
                    current = option;
                }
                options.add(option);
            }
        }
        options.forEach(cbPlanes::addItem);
        cbPlanes.setSelectedItem(current);
        refreshing = false;

        final JPanel panel = new JPanel(new MigLayout("insets 10, gap 0 8, wrap, ax center"));
        panel.setOpaque(false);
        panel.add(cbPlanes, "w 400px!, h 30px!, gapbottom 12");
        for (final FLabel label : new FLabel[] {lblAEtherShards, lblPlaneswalkEmblems, lblTotalWins, lblTotalLosses,
                lblConqueredEvents, lblUnlockedCards, lblCommanders, lblPlaneswalkers}) {
            panel.add(label, "w 400px!, h 34px!");
        }
        display.add(panel, CONTENT_CONSTRAINTS);
        model.updateStatLabels(this, model.getCurrentPlane());
    }

    @Override
    public IButton getLblAEtherShards() {
        return lblAEtherShards;
    }

    @Override
    public IButton getLblPlaneswalkEmblems() {
        return lblPlaneswalkEmblems;
    }

    @Override
    public IButton getLblTotalWins() {
        return lblTotalWins;
    }

    @Override
    public IButton getLblTotalLosses() {
        return lblTotalLosses;
    }

    @Override
    public IButton getLblConqueredEvents() {
        return lblConqueredEvents;
    }

    @Override
    public IButton getLblUnlockedCards() {
        return lblUnlockedCards;
    }

    @Override
    public IButton getLblCommanders() {
        return lblCommanders;
    }

    @Override
    public IButton getLblPlaneswalkers() {
        return lblPlaneswalkers;
    }
}
