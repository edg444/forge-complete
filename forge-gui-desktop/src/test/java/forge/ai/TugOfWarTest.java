package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tug of War (UNF #159), checked against Scryfall and its rulings 2026-10-21. A player with fewer than seven cards in
 * their library loses the subgame as it begins (rulings), which keeps this subgame short.
 */
public class TugOfWarTest extends AITest {

    @Test
    public void testWinnerKeepsASetAsideCard() {
        final Game game = initAndCreateGame();
        final Player winner = game.getPlayers().get(1);
        final Player loser = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", loser, ZoneType.Library);
        }
        addCardToZone("Serra Angel", winner, ZoneType.Library);
        addCardToZone("Grizzly Bears", winner, ZoneType.Library);
        addCardToZone("Llanowar Elves", winner, ZoneType.Library);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Forest", winner, ZoneType.Library);
        }
        final int libraryBefore = winner.getCardsIn(ZoneType.Library).size();

        final Card tug = addCardToZone("Tug of War", winner, ZoneType.Hand);
        final SpellAbility sa = tug.getFirstSpellAbility();
        sa.setActivatingPlayer(winner);
        AbilityUtils.resolve(sa);

        AssertJUnit.assertEquals("the best of its set-aside cards", 1,
                CardLists.filter(winner.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Serra Angel")).size());
        AssertJUnit.assertEquals(libraryBefore - 1, winner.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals("a subgame's loss isn't the main game's", 0, loser.getCardsIn(ZoneType.Battlefield).size());
        AssertJUnit.assertFalse(game.isGameOver());
    }
}
