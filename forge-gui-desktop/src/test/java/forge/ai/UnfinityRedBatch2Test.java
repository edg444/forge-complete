package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.SpellWordEffect;
import forge.game.card.Card;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Goblin Cruciverbalist (UNF #110), Goblin Girder Gang (UNF #111), Ignacio of Myra's Marvels (UNF #112) and Juggletron
 * (UNF #113), checked against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityRedBatch2Test extends AITest {

    @Test
    public void testJuggletronJugglesArtStickers() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card jug = addCard("Juggletron", p);
        final Card sheet = Card.fromPaperCard(StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        for (final Sticker s : StickerSheet.getStickers(sheet)) {
            if (s.getSlot().equals("STK4") || s.getSlot().equals("STK5") || s.getSlot().equals("STK3")) {
                jug.addSticker(new AppliedSticker(s, game.getNextTimestamp(), 0));
            }
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("two art stickers juggled; a name sticker isn't an item", 6, jug.getNetPower());
    }

    @Test
    public void testGirderGangStoresEachResultOnce() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gang = addCard("Goblin Girder Gang", ai);
        final SpellAbility store = gang.getTriggers().get(0).ensureAbility();
        store.setActivatingPlayer(ai);
        store.setTriggeringObject(AbilityKey.Result, 4);
        AbilityUtils.resolve(store);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, gang.getNetPower());
        AbilityUtils.resolve(store);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("a 4 is stored already", 1, gang.getNetPower());
        store.setTriggeringObject(AbilityKey.Result, 0);
        AbilityUtils.resolve(store);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("any result, even 0", 2, gang.getNetPower());
    }

    @Test
    public void testSpellingWithInitials() {
        // the app loads the word list at startup (FModel); tests don't start the app
        final java.io.File words = new java.io.File(forge.localinstance.properties.ForgeConstants.SPELLING_WORD_LIST_FILE);
        final String path = words.exists() ? words.getPath() : "../forge-gui/res/lists/SpellingWords.txt";
        forge.util.WordList.setSpellingSource(() -> forge.util.FileUtil.readFile(path));
        AssertJUnit.assertFalse("the word list loaded", forge.util.WordList.getSpelling(4).isEmpty());
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        addCard("Ornithopter", ai);
        addCard("Ancestral Recall", ai);
        addCard("Tundra", ai);
        // G, O, A, T: "goat", "toga"...
        AssertJUnit.assertTrue(SpellWordEffect.canSpell("GOAT", SpellWordEffect.initials(ai)));
        AssertJUnit.assertFalse("each letter once", SpellWordEffect.canSpell("GOGO", SpellWordEffect.initials(ai)));
        final Card cruci = addCard("Goblin Cruciverbalist", ai);
        final SpellAbility attack = cruci.getTriggers().get(1).ensureAbility();
        attack.setActivatingPlayer(ai);
        AbilityUtils.resolve(attack);
        game.getAction().checkStateEffects(true);
        final int first = cruci.getNetPower() - 1;
        AssertJUnit.assertTrue("the AI spells a real word from G, O, A, T and G (itself): " + first, first >= 3);
        final List<String> spelled = SpellWordEffect.spelled(ai);
        AssertJUnit.assertEquals(1, spelled.size());
        AbilityUtils.resolve(attack);
        AssertJUnit.assertEquals("never the same word twice", 2, SpellWordEffect.spelled(ai).size());
        AssertJUnit.assertFalse(SpellWordEffect.spelled(ai).get(0).equals(SpellWordEffect.spelled(ai).get(1)));
    }

    @Test
    public void testIgnacio() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card ignacio = addCard("Ignacio of Myra's Marvels", p);
        ignacio.setChosenType("fire");
        final Card spike = addCardToZone("Lava Spike", p, ZoneType.Hand);
        final Card bolt = addCardToZone("Fireball", p, ZoneType.Hand);
        AssertJUnit.assertTrue("Fireball's art has fire", bolt.isValid("Card.ArtHasChosenObject", p, ignacio, null));
        AssertJUnit.assertEquals("lava isn't fire unless Tagger says so",
                spike.hasKnownArtObject("fire"), spike.isValid("Card.ArtHasChosenObject", p, ignacio, null));
    }
}
