package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * It Came from Planet Glurg (UNF #169), checked against Scryfall and its rulings 2026-10-21: a copy of X creatures at
 * once - total power and toughness, combined mana costs, every ability, and every name.
 */
public class ItCameFromPlanetGlurgTest extends AITest {

    @Test
    public void testCopiesTwo() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        addCard("Serra Angel", ai);
        final Card glurg = addCardToZone("It Came from Planet Glurg", ai, ZoneType.Hand);
        final forge.game.spellability.SpellAbility cast = glurg.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.setXManaCostPaid(2);
        glurg.setCastSA(cast);
        game.getAction().moveToPlay(glurg, ai, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        final Card copy = game.getCardState(glurg);
        AssertJUnit.assertTrue(copy.isInPlay());
        AssertJUnit.assertEquals(6, copy.getNetPower());
        AssertJUnit.assertEquals(6, copy.getNetToughness());
        AssertJUnit.assertTrue(copy.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(copy.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertEquals("{1}{G} and {3}{W}{W}", 7, copy.getCMC());
        AssertJUnit.assertTrue(copy.getNames().contains("Grizzly Bears"));
        AssertJUnit.assertTrue(copy.getNames().contains("Serra Angel"));
        AssertJUnit.assertTrue(copy.sharesNameWith("Serra Angel"));
        AssertJUnit.assertEquals("each name's words", 4, AbilityUtils.wordsInName(copy));
    }
}
