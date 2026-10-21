package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.sticker.StickerKind;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Focused Funambulist (UNF #47), checked against Scryfall and its 4 rulings 2026-10-21: an art sticker as it enters is
 * optional, and only with one is a person asked whether it looks balanced.
 */
public class FocusedFunambulistTest extends AITest {

    @Test
    public void testNoStickerNoQuestion() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card f = addCardToZone("Focused Funambulist", p, ZoneType.Hand);
        game.getAction().moveToPlay(f, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertTrue("no sticker sheets, so no sticker and no reflexive trigger", game.getStack().isEmpty());
    }

    @Test
    public void testAStickerAsksTheQuestion() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCard("Serra Angel", game.getPlayers().get(0));
        p.getZone(ZoneType.StickerSheets).add(Card.fromPaperCard(
                StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), p));
        final Card f = addCardToZone("Focused Funambulist", p, ZoneType.Hand);
        final Card in = game.getAction().moveToPlay(f, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        final boolean stickered = in.getStickers().stream().anyMatch(s -> s.getKind() == StickerKind.ART);
        AssertJUnit.assertEquals("a reflexive trigger exactly when it got an art sticker", stickered, !game.getStack().isEmpty());
    }
}
