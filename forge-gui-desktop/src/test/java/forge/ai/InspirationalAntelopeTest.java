package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardDb;
import forge.deck.CardPool;
import forge.game.Game;
import forge.game.GameAction;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * Inspirational Antelope (CMB1 #78), checked against Scryfall and its 5 rulings 2026-10-15: legacy writes a keyword
 * ability or ability word, no modifier, on the physical card before the game starts, and it stays written.
 */
public class InspirationalAntelopeTest extends AITest {

    @Test
    public void theWordsOnOffer() {
        initAndCreateGame();
        List<String> words = GameAction.legacyWordChoices();
        AssertJUnit.assertTrue(words.contains("Flying"));
        AssertJUnit.assertTrue(words.contains("Cycling"));
        AssertJUnit.assertTrue(words.contains("landfall"));
        AssertJUnit.assertTrue(words.contains("will of the council"));
        AssertJUnit.assertTrue("the ruling's own example", words.contains("Legacy"));
        AssertJUnit.assertTrue(words.contains("Aggressive"));
        AssertJUnit.assertFalse("keyword actions aren't keyword abilities", words.contains("Scry"));
        AssertJUnit.assertEquals(words.size(), words.stream().map(String::toLowerCase).distinct().count());
    }

    @Test
    public void keywordsMatchWithoutModifiersAndAbilityWordsByText() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        AssertJUnit.assertTrue(addCard("Serra Angel", ai).hasKeywordOrAbilityWord("flying"));
        AssertJUnit.assertTrue(addCardToZone("Desert Cerodon", ai, ZoneType.Hand).hasKeywordOrAbilityWord("Cycling"));
        AssertJUnit.assertTrue(addCard("Steppe Lynx", ai).hasKeywordOrAbilityWord("landfall"));
        Card bears = addCard("Grizzly Bears", ai);
        AssertJUnit.assertFalse(bears.hasKeywordOrAbilityWord("flying"));
        AssertJUnit.assertFalse(bears.hasKeywordOrAbilityWord("landfall"));
    }

    @Test
    public void spellsWithTheWordCostOneLess() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Plains", 4, ai);
        Card angel = addCardToZone("Serra Angel", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        SpellAbility cast = angel.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(cast, ai, 0, false));

        Card antelope = addCard("Inspirational Antelope", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("a blank does nothing", ComputerUtilMana.canPayManaCost(cast, ai, 0, false));
        antelope.setLegacyWord("Flying");
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(cast, ai, 0, false));
    }

    @Test
    public void theAiWritesItsDecksWordAndItStaysOnTheCard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Wind Drake", ai, ZoneType.Library);
        }
        Card antelope = addCardToZone("Inspirational Antelope", ai, ZoneType.Library);
        PaperCard blank = (PaperCard) antelope.getPaperCard();
        ai.getRegisteredPlayer().getDeck().getMain().add(blank, 1);

        game.getAction().writeLegacyWord(ai, antelope, ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertEquals("Flying", antelope.getLegacyWord());

        CardPool main = ai.getRegisteredPlayer().getDeck().getMain();
        AssertJUnit.assertEquals(0, main.count(blank));
        PaperCard written = blank.copyWithLegacyWord("Flying");
        AssertJUnit.assertEquals(1, main.count(written));
        AssertJUnit.assertEquals(written, antelope.getPaperCard());

        // it survives the deck file, and a new game's card has it written from the start
        String line = CardDb.CardRequest.compose(written);
        AssertJUnit.assertTrue(line, line.contains("legacy=Flying"));
        PaperCard reread = CardPool.fromCardList(List.of("1 " + line)).toFlatList().get(0);
        AssertJUnit.assertEquals("Flying", reread.getLegacyWord());
        AssertJUnit.assertEquals("Flying", Card.fromPaperCard(reread, ai).getLegacyWord());

        // moving zones keeps it: it's the physical card
        Card onField = game.getAction().moveToPlay(antelope, null, null);
        AssertJUnit.assertEquals("Flying", onField.getLegacyWord());
    }
}
