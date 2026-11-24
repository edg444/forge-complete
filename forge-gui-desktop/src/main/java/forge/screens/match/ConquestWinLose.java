package forge.screens.match;

import forge.game.GameView;
import forge.gui.framework.EDocID;
import forge.model.FModel;
import forge.screens.home.CHomeUI;

/** Records a Planar Conquest battle's result and returns to the multiverse, which hands out the rewards. */
public class ConquestWinLose extends ControlWinLose {
    public ConquestWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
        view0.getBtnContinue().setVisible(false);
        view0.getBtnRestart().setVisible(false);
    }

    @Override
    public boolean populateCustomPanel() {
        FModel.getConquest().showGameOutcome(lastGame, getView());
        return false;
    }

    @Override
    public void actionOnQuit() {
        FModel.getConquest().finishEvent(getView());
        super.actionOnQuit();
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_CONQUESTMULTIVERSE);
    }
}
