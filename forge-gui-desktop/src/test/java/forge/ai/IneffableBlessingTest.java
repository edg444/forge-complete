package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.ArtistCredit;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Ineffable Blessing (Unstable 113a-f), six functional variants - Oracle text and Scryfall rulings for each checked
 * 2026-10-03, plus the Unstable FAQ entry (its full-art border question was published without an answer).
 * Test printings, checked against Scryfall and the fork's printing data: Druid of the Sacred Beaker (UST 106) has
 * flavor text, is uncommon, even, five words, by Simon Dominic; Eager Beaver (UST 107) has none, is common, odd,
 * two words, by Andrea Radeck; Fourth Edition's Grizzly Bears is white-bordered.
 */
public class IneffableBlessingTest extends AITest {

    private Card printing(String name, String set, String cn, Player p, ZoneType zone) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    private Card blessing(Game game, Player me, String variant) {
        Card b = printing("Ineffable Blessing", "UST", "113" + variant, me, ZoneType.Hand);
        b = game.getAction().moveToPlay(b, me, null, null);
        game.getAction().checkStateEffects(true);
        return b;
    }

    /** How many cards a creature's entering draws. */
    private int draws(Game game, Player me, String name, String set, String cn) {
        Card c = printing(name, set, cn, me, ZoneType.Hand);
        int before = me.getCardsIn(ZoneType.Hand).size() - 1;
        game.getAction().moveToPlay(c, me, null, null);
        playUntilStackClear(game);
        return me.getCardsIn(ZoneType.Hand).size() - before;
    }

    private int druid(Game game, Player me) {
        return draws(game, me, "Druid of the Sacred Beaker", "UST", "106");
    }

    private int beaver(Game game, Player me) {
        return draws(game, me, "Eager Beaver", "UST", "107");
    }

    private Player setUp(Game game) {
        Player me = game.getPlayers().get(1);
        fillLibrary(me, 20);
        moveToMain2(game, me);
        return me;
    }

    @Test
    public void testEachVariantReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        String[][] texts = {
            {"a", "• Flavorful — Whenever a creature you control with flavor text enters, draw a card.\r\n"
                    + "• Bland — Whenever a creature you control without flavor text enters, draw a card."},
            {"b", "Whenever a creature you control with art by the chosen artist enters, draw a card."},
            {"c", "Whenever a creature you control with the chosen border enters, draw a card."},
            {"d", "Whenever a creature you control with the chosen rarity enters, draw a card."},
            {"e", "Whenever a creature you control with a collector number of the chosen value enters, draw a card."},
            {"f", "Whenever a creature you control with exactly the chosen number of words in its name enters, draw a card. (Hyphenated words are one word.)"},
        };
        for (String[] t : texts) {
            Card b = printing("Ineffable Blessing", "UST", "113" + t[0], me, ZoneType.Battlefield);
            // abilities are separated by a blank line
            String text = b.getAbilityText().replace("\r\n", "\n").replaceAll("\n+", "\n").trim();
            // one printed line per ability: the per-choice triggers past the first are Secondary
            AssertJUnit.assertTrue(t[0] + ": " + text, text.endsWith(t[1].replace("\r\n", "\n")));
            AssertJUnit.assertTrue(t[0] + ": " + text, text.startsWith("As "));
        }
    }

    @Test
    public void testFlavorfulAndBland() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "a");
        AssertJUnit.assertFalse(b.getChosenMode().isEmpty());
        b.setChosenMode("Flavorful");
        AssertJUnit.assertEquals(1, druid(game, me));
        AssertJUnit.assertEquals(0, beaver(game, me));
        b.setChosenMode("Bland");
        AssertJUnit.assertEquals(0, druid(game, me));
        AssertJUnit.assertEquals(1, beaver(game, me));
    }

    @Test
    public void testAiPicksWhatItsCreaturesHave() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            printing("Druid of the Sacred Beaker", "UST", "106", me, ZoneType.Library);
        }
        printing("Eager Beaver", "UST", "107", me, ZoneType.Library);
        moveToMain2(game, me);
        AssertJUnit.assertEquals("Flavorful", blessing(game, me, "a").getChosenMode());
        AssertJUnit.assertEquals("Uncommon", blessing(game, me, "d").getChosenMode());
        AssertJUnit.assertEquals("Even", blessing(game, me, "e").getChosenMode());
        AssertJUnit.assertEquals("Silver-bordered", blessing(game, me, "c").getChosenMode());
        AssertJUnit.assertEquals("Simon Dominic", blessing(game, me, "b").getChosenArtist());
        AssertJUnit.assertEquals(Integer.valueOf(5), blessing(game, me, "f").getChosenNumber());
    }

    @Test
    public void testArtist() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "b");
        b.setChosenArtist("Andrea Radeck");
        AssertJUnit.assertEquals(0, druid(game, me));
        AssertJUnit.assertEquals(1, beaver(game, me));
    }

    @Test
    public void testArtistNicknamesAndSharedCredits() {
        // ruling: nicknames printed on earlier silver-bordered cards are ignored
        AssertJUnit.assertTrue(ArtistCredit.credits("Rebecca \"Don't Mess with Me\" Guay", "Rebecca Guay"));
        AssertJUnit.assertEquals("Lars Grant-West", ArtistCredit.withoutNickname("Lars Grant-\"Wild Wild\"-West"));
        AssertJUnit.assertEquals("Edward P. Beard, Jr.", ArtistCredit.withoutNickname("Edward P. \"Feed Me\" Beard, Jr."));
        // a printing credited to two artists is by each of them
        AssertJUnit.assertTrue(ArtistCredit.credits("Aaron J. Riley & Eric Wilkerson", "Eric Wilkerson"));
        AssertJUnit.assertFalse(ArtistCredit.credits("Aaron J. Riley & Eric Wilkerson", "Eric"));
    }

    @Test
    public void testBorder() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "c");
        b.setChosenMode("White-bordered");
        AssertJUnit.assertEquals(1, draws(game, me, "Grizzly Bears", "4ED", "250"));
        AssertJUnit.assertEquals(0, druid(game, me));
        b.setChosenMode("Silver-bordered");
        AssertJUnit.assertEquals(0, draws(game, me, "Grizzly Bears", "4ED", "250"));
        AssertJUnit.assertEquals(1, druid(game, me));
    }

    @Test
    public void testRarity() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "d");
        b.setChosenMode("Common");
        AssertJUnit.assertEquals(0, druid(game, me));
        AssertJUnit.assertEquals(1, beaver(game, me));
        b.setChosenMode("Uncommon");
        AssertJUnit.assertEquals(1, druid(game, me));
        AssertJUnit.assertEquals(0, beaver(game, me));
        b.setChosenMode("Mythic Rare");
        AssertJUnit.assertEquals(0, druid(game, me));
    }

    @Test
    public void testOddOrEven() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "e");
        b.setChosenMode("Odd");
        AssertJUnit.assertEquals(0, druid(game, me));
        AssertJUnit.assertEquals(1, beaver(game, me));
        b.setChosenMode("Even");
        AssertJUnit.assertEquals(1, druid(game, me));
        AssertJUnit.assertEquals(0, beaver(game, me));
    }

    @Test
    public void testWordsInName() {
        Game game = initAndCreateGame();
        Player me = setUp(game);
        Card b = blessing(game, me, "f");
        b.setChosenNumber(2);
        AssertJUnit.assertEquals(0, druid(game, me));
        AssertJUnit.assertEquals(1, beaver(game, me));
        b.setChosenNumber(5);
        AssertJUnit.assertEquals(1, druid(game, me));
    }
}
