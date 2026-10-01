package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spike, Tournament Grinder (Unstable): "{B/P}{B/P}{B/P}{B/P}: Reveal a card you own from outside the game that has
 * been banned or restricted in a Constructed format and put it into your hand." - Oracle text and ruling checked
 * against Scryfall 2026-10-01; the list is the Unstable FAQ's (December 2017) plus Scryfall's current bans.
 */
public class SpikeTournamentGrinderTest extends AITest {

    @Test
    public void testWhatCounts() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card spike = addCard("Spike, Tournament Grinder", me);
        String[][] cases = {
            {"Black Lotus", "true"},             // the FAQ's list, and restricted in Vintage
            {"Oko, Thief of Crowns", "true"},    // banned today (Pioneer, Modern, Legacy...)
            {"Baron Sengir", "true"},            // a legendary card first printed in Homelands
            {"Erayo, Soratami Ascendant", "true"}, // the FAQ's list, a flip card
            {"Grizzly Bears", "false"},
        };
        for (String[] c : cases) {
            Card card = addCardToZone(c[0], me, ZoneType.Sideboard);
            AssertJUnit.assertEquals(c[0], Boolean.parseBoolean(c[1]),
                    card.isValid("Card.YouOwn+everBannedOrRestricted", me, spike, null));
        }
        // conspiracies count too (the FAQ), though they're kept with the variant cards
        AssertJUnit.assertTrue(forge.card.BannedOrRestricted.contains("Backup Plan"));
    }

    @Test
    public void testFetchFromOutsideTheGame() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card spike = addCard("Spike, Tournament Grinder", me);
        Card bears = addCardToZone("Grizzly Bears", me, ZoneType.Sideboard);
        Card lotus = addCardToZone("Black Lotus", me, ZoneType.Sideboard);
        AssertJUnit.assertEquals("{B/P}{B/P}{B/P}{B/P}: Reveal a card you own from outside the game that has been "
                + "banned or restricted in a Constructed format and put it into your hand.", spike.getAbilityText().trim());

        SpellAbility sa = null;
        for (SpellAbility s : spike.getSpellAbilities()) {
            if (s.getApi() == ApiType.ChangeZone) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(game.getCardState(lotus)));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Sideboard).contains(bears));
    }
}
