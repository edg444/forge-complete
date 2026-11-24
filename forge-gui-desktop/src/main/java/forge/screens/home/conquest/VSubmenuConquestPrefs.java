package forge.screens.home.conquest;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gui.framework.EDocID;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import net.miginfocom.swing.MigLayout;

/** Planar Conquest tuning values: shard economy, booster contents, emblem costs and chaos battle difficulty. */
public final class VSubmenuConquestPrefs extends AConquestSubmenu {
    public static final VSubmenuConquestPrefs INSTANCE = new VSubmenuConquestPrefs();

    private final JPanel pnlPrefs = new JPanel(new MigLayout("insets 10, gap 6, wrap 2", "[grow, fill][100px!]"));
    private final List<PrefField> fields = new ArrayList<>();

    private VSubmenuConquestPrefs() {
        super("lblConquestPreference", false);
        pnlPrefs.setOpaque(false);
        addHeader("lblAetherShards", FSkinProp.IMG_AETHER_SHARD);
        addOption("lblBaseDuplicateValue", CQPref.AETHER_BASE_DUPLICATE_VALUE, false);
        addOption("lblBaseExileValue", CQPref.AETHER_BASE_EXILE_VALUE, false);
        addOption("lblBaseRetrieveCost", CQPref.AETHER_BASE_RETRIEVE_COST, false);
        addOption("lblBasePullCost", CQPref.AETHER_BASE_PULL_COST, false);
        addOption("lblUncommonMultiplier", CQPref.AETHER_UNCOMMON_MULTIPLIER, false);
        addOption("lblRareMultiplier", CQPref.AETHER_RARE_MULTIPLIER, false);
        addOption("lblMythicMultiplier", CQPref.AETHER_MYTHIC_MULTIPLIER, false);
        addOption("lblStartingShards", CQPref.AETHER_START_SHARDS, false);
        addOption("lblChaosWheelShardValue", CQPref.AETHER_WHEEL_SHARDS, false);

        addHeader("lblBoosterPacks", FSkinProp.IMG_PACK);
        addOption("lblCommons", CQPref.BOOSTER_COMMONS, true);
        addOption("lblUncommons", CQPref.BOOSTER_UNCOMMONS, true);
        addOption("lblRares", CQPref.BOOSTER_RARES, true);
        addOption("lblBoostersPerMythic", CQPref.BOOSTERS_PER_MYTHIC, true);

        addHeader("lblPlaneswalkEmblems", FSkinProp.IMG_PW_BADGE_COMMON);
        addOption("lblBaseConquerReward", CQPref.PLANESWALK_CONQUER_EMBLEMS, false);
        addOption("lblChaosWheelBonus", CQPref.PLANESWALK_WHEEL_EMBLEMS, false);
        addOption("lblFirstPlaneUnlockCost", CQPref.PLANESWALK_FIRST_UNLOCK, false);
        addOption("lblCostIncreasePerUnlock", CQPref.PLANESWALK_UNLOCK_INCREASE, false);

        addHeader("lblChaosBattles", FSkinProp.IMG_CHAOS);
        addOption("lblWinsforMediumAI", CQPref.CHAOS_BATTLE_WINS_MEDIUMAI, false);
        addOption("lblWinsforHardAI", CQPref.CHAOS_BATTLE_WINS_HARDAI, false);
        addOption("lblWinsforExpertAI", CQPref.CHAOS_BATTLE_WINS_EXPERTAI, false);
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONQUESTPREFS;
    }

    @Override
    protected void populateContent(final JPanel display) {
        final ConquestPreferences prefs = FModel.getConquestPreferences();
        for (final PrefField field : fields) {
            field.text.setText(prefs.getPref(field.pref));
        }
        final JPanel wrapper = new JPanel(new MigLayout("insets 0, gap 0, ax center"));
        wrapper.setOpaque(false);
        wrapper.add(pnlPrefs, "w 600px!");
        final FScrollPane scroller = new FScrollPane(wrapper, false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        display.add(scroller, CONTENT_CONSTRAINTS);
    }

    private void addHeader(final String titleKey, final FSkinProp icon) {
        final FLabel header = new FLabel.Builder().text(localizer.getMessage(titleKey)).fontSize(16)
                .icon(ConquestSkin.getIcon(icon, 24)).build();
        pnlPrefs.add(header, "span 2, h 32px!, gaptop 10");
    }

    private void addOption(final String labelKey, final CQPref pref, final boolean booster) {
        final PrefField field = new PrefField(pref, booster);
        fields.add(field);
        pnlPrefs.add(new FLabel.Builder().text(localizer.getMessage(labelKey)).fontSize(13).build(), "h 26px!, gapleft 20");
        pnlPrefs.add(field.text, "h 26px!");
    }

    private final class PrefField {
        private final CQPref pref;
        private final boolean booster;
        private final FTextField text = new FTextField.Builder().maxLength(6).build();

        private PrefField(final CQPref pref0, final boolean booster0) {
            pref = pref0;
            booster = booster0;
            text.addActionListener(e -> commit());
            text.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    commit();
                }
            });
        }

        private void commit() {
            final ConquestPreferences prefs = FModel.getConquestPreferences();
            final String saved = prefs.getPref(pref);
            final String value = text.getText().trim();
            if (value.equals(saved)) {
                return;
            }
            final int val;
            try {
                val = Integer.parseInt(value);
            } catch (final NumberFormatException ex) {
                text.setText(saved);
                return;
            }
            final String validationError = prefs.validatePreference(pref, val);
            if (validationError != null) {
                text.setText(saved);
                FOptionPane.showErrorDialog(validationError,
                        localizer.getMessage("lblSaveFailed") + (booster ? localizer.getMessage("lblBoosterPacks") : ""));
                return;
            }
            prefs.setPref(pref, String.valueOf(val));
            prefs.save();
            text.setText(String.valueOf(val));
        }
    }
}
