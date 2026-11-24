package forge.screens.home.conquest;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Base for the Planar Conquest home submenus. Each screen is its own view and controller (the
 * screens are small enough that splitting them like the older modes would only add indirection).
 */
public abstract class AConquestSubmenu implements IVSubmenu<AConquestSubmenu>, ICDoc {
    protected static final String CONTENT_CONSTRAINTS = "w 98%!, growy, pushy, gap 1% 0 0 15px";

    protected final Localizer localizer = Localizer.getInstance();
    private final DragTab tab;
    private final String menuTitle;
    private final boolean needsConquest;
    private DragCell parentCell;
    protected final FLabel lblTitle;

    protected AConquestSubmenu(final String menuTitleKey, final boolean needsConquest0) {
        menuTitle = localizer.getMessage(menuTitleKey);
        needsConquest = needsConquest0;
        tab = new DragTab(menuTitle);
        lblTitle = new FLabel.Builder().text(menuTitle).fontAlign(SwingConstants.CENTER).opaque(true).fontSize(16).build();
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    }

    /** Adds this screen's content to the home display panel (MigLayout, one column) below the title. */
    protected abstract void populateContent(JPanel display);

    @Override
    public final void populate() {
        final VHomeUI.PnlDisplay display = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
        display.removeAll();
        display.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        display.add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        if (needsConquest && !ConquestDesktop.ensureConquest()) {
            lblTitle.setText(menuTitle);
            display.add(buildNoConquestPanel(), CONTENT_CONSTRAINTS);
        } else {
            lblTitle.setText(needsConquest ? menuTitle + " - " + ConquestDesktop.model().getName() : menuTitle);
            populateContent(display);
        }
        display.repaintSelf();
        display.revalidate();
    }

    private JPanel buildNoConquestPanel() {
        final JPanel panel = new JPanel(new MigLayout("insets 0, gap 10, wrap, ax center, ay center"));
        panel.setOpaque(false);
        panel.add(new FLabel.Builder().text(localizer.getMessage("lblNoActiveConquest")).fontSize(15)
                .fontAlign(SwingConstants.CENTER).build(), "ax center, gapbottom 10");
        final FButton btnNew = new FButton(localizer.getMessage("lblNewConquest"));
        btnNew.setCommand(() -> ConquestDesktop.open(EDocID.HOME_CONQUESTNEW));
        final FButton btnLoad = new FButton(localizer.getMessage("lblLoadConquest"));
        btnLoad.setCommand(() -> ConquestDesktop.open(EDocID.HOME_CONQUESTLOAD));
        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 10"));
        buttons.setOpaque(false);
        buttons.add(btnNew, "w 200px!, h 36px!");
        buttons.add(btnLoad, "w 200px!, h 36px!");
        panel.add(buttons, "ax center");
        return panel;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.CONQUEST;
    }

    @Override
    public String getMenuTitle() {
        return menuTitle;
    }

    @Override
    public EDocID getItemEnum() {
        return getDocumentID();
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public AConquestSubmenu getLayoutControl() {
        return this;
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
    public void register() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
    }
}
