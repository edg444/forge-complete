package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Goblin Haberdasher (Unstable): "Menace / Other creatures you control wearing hats in their art have menace." - Oracle
 * text, rulings and the Unstable FAQ entry checked 2026-10-02. A hat the printing data confirms gives menace outright;
 * otherwise the controller is asked when the creature attacks. Every art below was checked by eye.
 */
public class GoblinHaberdasherTest extends AITest {

    private Card printing(String name, String set, String cn, Player p) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card goblin = addCard("Goblin Haberdasher", game.getPlayers().get(1));
        // evergreen keywords print without their reminder text in Forge
        AssertJUnit.assertEquals("Menace | Other creatures you control wearing hats in their art have menace.",
                String.join(" | ", goblin.getAbilityText().trim().split("\\s*[\\r\\n]+")));
        AssertJUnit.assertTrue(goblin.getType().hasCreatureType("Hatificer"));
    }

    @Test
    public void testConfirmedHatsHaveMenace() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // a flat cap on single-figure art
        Card newsie = printing("Snooping Newsie", "SNC", "222", me);
        Card theirs = printing("Snooping Newsie", "SNC", "222", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(newsie.hasKnownHatInArt());
        AssertJUnit.assertFalse(newsie.hasKeyword(Keyword.MENACE));

        addCard("Goblin Haberdasher", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(newsie.hasKeyword(Keyword.MENACE));
        AssertJUnit.assertFalse(theirs.hasKeyword(Keyword.MENACE));
    }

    @Test
    public void testHeldHelmetIsNotConfirmed() {
        Game game = initAndCreateGame();
        // she carries her helmet; Tagger's plain "helmet" tag can't tell worn from held
        Card marshal = printing("Seasoned Marshal", "8ED", "44", game.getPlayers().get(1));
        AssertJUnit.assertFalse(marshal.hasKnownHatInArt());
    }

    @Test
    public void testUnconfirmedHatIsAskedWhenItAttacks() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card marshal = printing("Seasoned Marshal", "8ED", "44", me);
        game.getAction().checkStateEffects(true);

        // nothing to ask about without Haberdasher
        game.getAction().askHatInArt(List.of(marshal));
        AssertJUnit.assertNull(marshal.getHatInArtClaim());

        addCard("Goblin Haberdasher", me);
        game.getAction().checkStateEffects(true);
        // the AI can't look at the art, so it never claims a hat
        game.getAction().askHatInArt(List.of(marshal));
        AssertJUnit.assertEquals(Boolean.FALSE, marshal.getHatInArtClaim());
        AssertJUnit.assertFalse(marshal.hasKeyword(Keyword.MENACE));

        // a player who says yes gets menace for it
        marshal.setHatInArtClaim(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(marshal.hasKeyword(Keyword.MENACE));
    }

    @Test
    public void testAnswerStaysWithTheCard() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card marshal = printing("Seasoned Marshal", "8ED", "44", me);
        marshal.setHatInArtClaim(true);

        Card inGraveyard = game.getAction().moveToGraveyard(marshal, null);
        AssertJUnit.assertEquals(Boolean.TRUE, inGraveyard.getHatInArtClaim());
    }
}
