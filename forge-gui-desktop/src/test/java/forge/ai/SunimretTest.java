package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sunimret (CMB1 #47), checked against Scryfall and rulings 2026-10-13: reverse miracle lets you cast it for {B} if
 * it's the bottom card of your library as you begin searching it. The AI takes the offer when exiling all creatures
 * is worth it right now.
 */
public class SunimretTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private Game searchWith(boolean onBottom) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Swamp", 5, ai); // four for the tutor, one for the reverse miracle
        if (onBottom) { // cards added later go under the earlier ones
            fillLibrary(ai, 5);
            addCardToZone("Sunimret", ai, ZoneType.Library);
        } else {
            fillLibrary(ai, 2);
            addCardToZone("Sunimret", ai, ZoneType.Library);
            fillLibrary(ai, 3);
        }
        addCard("Craw Wurm", opp);
        addCard("Serra Angel", opp);
        Card tutor = addCardToZone("Diabolic Tutor", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = tutor.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        resolveAll(game);
        return game;
    }

    @Test
    public void castFromTheBottomWhenTheSearchBegins() {
        Game game = searchWith(true);
        AssertJUnit.assertEquals(0, forge.game.card.CardLists.count(game.getCardsIn(ZoneType.Battlefield), Card::isCreature));
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Sunimret", ZoneType.Graveyard));
    }

    @Test
    public void notWhenItIsntTheBottomCard() {
        Game game = searchWith(false);
        AssertJUnit.assertEquals(2, forge.game.card.CardLists.count(game.getCardsIn(ZoneType.Battlefield), Card::isCreature));
    }

    @Test
    public void rulesTextShowsTheKeyword() {
        Game game = initAndCreateGame();
        Card card = addCardToZone("Sunimret", game.getPlayers().get(1), ZoneType.Hand);
        String text = card.getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Reverse miracle {B} (If this card is on the bottom of your library as you begin searching it, you may cast it by paying its reverse miracle cost.)"));
    }
}
