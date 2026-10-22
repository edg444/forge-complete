package forge.screens.match.views;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.menu.FDropDown;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

/**
 * Advanced Tactics: its 5x5 grid, dropped down from the match menu bar while there is one. Each space shows its
 * creature's art, name and power/toughness, yours framed blue and opponents' red; tapping one zooms the card.
 */
public class VTacticsGrid extends FDropDown {
    private static final int SIZE = 5;
    private static final float MARGINS = Utils.scale(4);
    private static final FSkinFont FONT = FSkinFont.get(10);
    private static final Color MINE = new Color(0.27f, 0.43f, 0.67f, 1f);
    private static final Color THEIRS = new Color(0.63f, 0.27f, 0.27f, 1f);
    private static final Color EMPTY = new Color(0.24f, 0.24f, 0.24f, 1f);

    private CardView host;

    public void setHost(final CardView host0) {
        host = host0;
    }

    /** Whether there's a grid to show. */
    public boolean hasGrid() {
        return host != null && !host.getTacticsGrid().isEmpty() && host.getZone() == ZoneType.Battlefield;
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected void drawBackground(final Graphics g) {
        g.fillRect(EMPTY.cpy().mul(0.6f, 0.6f, 0.6f, 1f), 0, 0, getWidth(), getHeight());
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(final float maxWidth, final float maxVisibleHeight) {
        clear();
        final float width = Math.min(Forge.getScreenWidth(), maxWidth);
        final float cellW = (width - (SIZE + 1) * MARGINS) / SIZE;
        final float cellH = Math.min(cellW * 0.8f, (maxVisibleHeight - (SIZE + 1) * MARGINS) / SIZE);
        final Map<Integer, CardView> onBattlefield = new HashMap<>();
        for (final PlayerView p : MatchController.instance.getGameView().getPlayers()) {
            for (final CardView c : p.getCards(ZoneType.Battlefield)) {
                onBattlefield.put(c.getId(), c);
            }
        }
        final String[] ids = hasGrid() ? host.getTacticsGrid().split(",") : new String[0];
        for (int i = 0; i < SIZE * SIZE; i++) {
            final CardView c = i < ids.length ? onBattlefield.get(Integer.parseInt(ids[i].trim())) : null;
            final GridCell cell = add(new GridCell(c));
            cell.setBounds(MARGINS + (i % SIZE) * (cellW + MARGINS), MARGINS + (i / SIZE) * (cellH + MARGINS), cellW, cellH);
        }
        return new ScrollBounds(width, SIZE * (cellH + MARGINS) + MARGINS);
    }

    private static class GridCell extends FDisplayObject {
        private final CardView card;
        private FImageComplex art;

        private GridCell(final CardView card0) {
            card = card0;
        }

        @Override
        public boolean tap(final float x, final float y, final int count) {
            if (card != null) {
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public void draw(final Graphics g) {
            final float w = getWidth();
            final float h = getHeight();
            if (card == null) {
                g.fillRect(EMPTY, 0, 0, w, h);
                return;
            }
            final PlayerView controller = card.getController();
            g.fillRect(controller != null && MatchController.instance.isLocalPlayer(controller) ? MINE : THEIRS, 0, 0, w, h);
            final float inset = Utils.scale(2);
            if (art == null) {
                art = CardRenderer.getCardArt(card);
            }
            if (art != null) {
                g.drawImage(art, inset, inset, w - 2 * inset, h - 2 * inset);
            }
            final String text = card.getName() + "\n" + card.getCurrentState().getPower() + "/" + card.getCurrentState().getToughness();
            g.fillRect(Color.BLACK.cpy().mul(1f, 1f, 1f, 0.55f), inset, h * 0.55f, w - 2 * inset, h * 0.45f - inset);
            g.drawText(text, FONT, Color.WHITE, inset, h * 0.55f, w - 2 * inset, h * 0.45f - inset, true, Align.center, true);
        }
    }
}
