package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Selfie Preservation (Unstable): "Search your library for a basic land card and reveal it. If there's a tree in its
 * art, put it onto the battlefield tapped. Otherwise, put it into your hand. Then shuffle." - Oracle text, the two
 * Scryfall rulings and the Unstable FAQ entry checked 2026-10-03. Magic 2010's Forest 246 is tagged with a tree by
 * Scryfall Tagger; its Island 234 isn't tagged either way, so that one is asked about (the AI answers no).
 */
public class SelfiePreservationTest extends AITest {

    private Card printing(String name, String set, String cn, Player p, ZoneType zone) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    private Card castWithOnly(String land, String cn) {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card basic = printing(land, "M10", cn, me, ZoneType.Library);
        Card selfie = addCardToZone("Selfie Preservation", me, ZoneType.Hand);
        SpellAbility sa = selfie.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
        return game.getCardState(basic);
    }

    @Test
    public void testTaggedTreeGoesOntoTheBattlefieldTapped() {
        Card forest = castWithOnly("Forest", "246");
        AssertJUnit.assertTrue(forest.hasKnownTreeInArt());
        AssertJUnit.assertTrue(forest.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(forest.isTapped());
    }

    @Test
    public void testUntaggedArtIsAskedAbout() {
        Card island = castWithOnly("Island", "234");
        AssertJUnit.assertFalse(island.hasKnownTreeInArt());
        // the AI can't see the art, so it says there's no tree
        AssertJUnit.assertTrue(island.isInZone(ZoneType.Hand));
    }
}
