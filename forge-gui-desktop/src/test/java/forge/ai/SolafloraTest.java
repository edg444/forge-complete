package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerSheet;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Solaflora, Intergalactic Icon (UNF #27), checked against Scryfall and its 3 rulings 2026-10-21: her Auras and
 * Equipment, counters and stickers affect your other creatures without their being enchanted, equipped, countered or
 * stickered; a name sticker's word goes into their names at the same spot.
 */
public class SolafloraTest extends AITest {

    private Sticker sticker(final Player p, final String slot) {
        final Card sheet = Card.fromPaperCard(StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        for (final Sticker s : StickerSheet.getStickers(sheet)) {
            if (s.getSlot().equals(slot)) {
                return s;
            }
        }
        throw new AssertionError(slot);
    }

    @Test
    public void testHerEquipmentAndAurasButOnlyYours() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card sola = addCard("Solaflora, Intergalactic Icon", p);
        final Card bears = addCard("Grizzly Bears", p);
        bears.setSickness(false);
        final Card theirs = addCard("Llanowar Elves", opp);
        addCard("Bonesplitter", p).attachToEntity(sola, null);
        addCard("Pacifism", opp).attachToEntity(sola, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertEquals(2, bears.getNetToughness());
        AssertJUnit.assertFalse("not actually equipped", bears.isEquipped());
        AssertJUnit.assertEquals("only your creatures", 1, theirs.getNetPower());
        AssertJUnit.assertTrue("an opponent's Aura isn't shared", CombatUtil.canAttack(bears));
        AssertJUnit.assertEquals("she has her own Bonesplitter", 5, sola.getNetPower());

        addCard("Holy Strength", p).attachToEntity(sola, null);
        final Card sword = addCard("Sword of Fire and Ice", p);
        sword.attachToEntity(sola, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("an Aura too", 2 + 2 + 1 + 2, bears.getNetPower());
        AssertJUnit.assertTrue("equipment triggers see it as the equipped creature",
                bears.isValid("Creature.EquippedBy", p, sword, null));
        AssertJUnit.assertFalse(theirs.isValid("Creature.EquippedBy", p, sword, null));
    }

    @Test
    public void testHerCounters() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card sola = addCard("Solaflora, Intergalactic Icon", p);
        final Card bears = addCard("Grizzly Bears", p);
        sola.addCounterInternal(CounterEnumType.P1P1, 2, p, false, null, null);
        sola.addCounterInternal(CounterType.getType("Flying"), 1, p, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertEquals(4, bears.getNetToughness());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse("no counters on it", bears.hasCounters());
    }

    @Test
    public void testHerStickers() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card sola = addCard("Solaflora, Intergalactic Icon", p);
        final Card bears = addCard("Grizzly Bears", p);
        sola.addSticker(new AppliedSticker(sticker(p, "STK3"), game.getNextTimestamp(), 1));
        sola.addSticker(new AppliedSticker(sticker(p, "STK8"), game.getNextTimestamp()));
        sola.addSticker(new AppliedSticker(sticker(p, "STK9"), game.getNextTimestamp()));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Solaflora, Minotaur Intergalactic Icon", sola.getName());
        AssertJUnit.assertEquals("the same spot in its name", "Grizzly Minotaur Bears", bears.getName());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertEquals(1, bears.getNetPower());
        AssertJUnit.assertEquals(4, bears.getNetToughness());
        AssertJUnit.assertFalse("no stickers on it", bears.isStickered());
    }
}
