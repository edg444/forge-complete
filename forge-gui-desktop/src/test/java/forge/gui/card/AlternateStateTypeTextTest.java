package forge.gui.card;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;

/** The second half of an Adventure-style card, in the card's details: its type line comes before its name. */
public class AlternateStateTypeTextTest extends AITest {
    private void assertShows(final String cardName, final String expected) {
        final Game game = initAndCreateGame();
        final Card card = addCard(cardName, game.getPlayers().get(0));
        final String text = CardDetailUtil.composeCardText(card.getView().getCurrentState(), game.getView(), true);
        AssertJUnit.assertTrue(text, text.contains(expected));
    }

    @Test
    public void anAdventureShowsItsSpellType() {
        assertShows("Bonecrusher Giant", "Instant — Adventure — Stomp ");
    }

    @Test
    public void anOmenShowsItsSpellType() {
        assertShows("Bloomvine Regent", "Sorcery — Omen — Claim Territory ");
    }

    @Test
    public void aPreparedSpellShowsItsSpellType() {
        assertShows("Adventurous Eater", "Prepared — Sorcery — Have a Bite ");
    }
}
