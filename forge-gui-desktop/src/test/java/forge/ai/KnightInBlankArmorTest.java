package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Knight in _____ Armor (UNF #17), checked against Scryfall and its ruling 2026-10-21: protection from names that
 * start with the same letter as a name sticker on it, skipping spaces and punctuation.
 */
public class KnightInBlankArmorTest extends AITest {

    private Sticker sticker(final Player p, final String sheetName, final String slot) {
        final Card sheet = Card.fromPaperCard(StaticData.instance().getVariantCards().getCard(sheetName), p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        for (final Sticker s : StickerSheet.getStickers(sheet)) {
            if (s.getSlot().equals(slot)) {
                return s;
            }
        }
        throw new AssertionError(sheetName + " has no sticker " + slot);
    }

    @Test
    public void testProtectionFromTheStickersLetter() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card knight = addCard("Knight in _____ Armor", p);
        final Card fanatic = addCard("Mogg Fanatic", opp);
        final Card goblin = addCard("_____ Goblin", opp);
        final Card jet = addCardToZone("Magma Jet", opp, ZoneType.Hand);
        final Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility jetSa = jet.getFirstSpellAbility();
        jetSa.setActivatingPlayer(opp);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(opp);
        AssertJUnit.assertTrue("no sticker, no protection", jetSa.canTarget(knight));
        AssertJUnit.assertTrue(CombatUtil.canBlock(knight, fanatic));

        knight.addSticker(new AppliedSticker(sticker(p, "Ancestral Hot Dog Minotaur", "STK3"), game.getNextTimestamp(), 0));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("Minotaur: protection from M", jetSa.canTarget(knight));
        AssertJUnit.assertFalse(CombatUtil.canBlock(knight, fanatic));
        AssertJUnit.assertTrue(boltSa.canTarget(knight));
        AssertJUnit.assertTrue("_____ Goblin starts with G", CombatUtil.canBlock(knight, goblin));
    }

    @Test
    public void testItMayGetANameStickerAsItEnters() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        sticker(p, "Ancestral Hot Dog Minotaur", "STK1");
        final Card knight = addCardToZone("Knight in _____ Armor", p, ZoneType.Hand);
        final Card played = game.getAction().moveToPlay(knight, null, null);
        boolean named = false;
        for (final AppliedSticker s : played.getStickers()) {
            named |= s.getKind() == StickerKind.NAME;
        }
        AssertJUnit.assertTrue("the AI puts one on", named);
    }
}
