package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Baron Von Count (Unstable) - Oracle text, the four Scryfall rulings and the Unstable FAQ entry checked 2026-10-03.
 */
public class BaronVonCountTest extends AITest {

    private Card baron(Game game, Player p) {
        Card b = addCardToZone("Baron Von Count", p, ZoneType.Hand);
        b = game.getAction().moveToPlay(b, p, null, null);
        game.getAction().checkStateEffects(true);
        return b;
    }

    /** Casts a spell from hand through the stack and lets everything resolve. */
    private void cast(Game game, Player p, String name) {
        Card c = addCardToZone(name, p, ZoneType.Hand);
        c.getFirstSpellAbility().setActivatingPlayer(p);
        game.getStack().add(c.getFirstSpellAbility());
        playUntilStackClear(game);
    }

    @Test
    public void testEntersOnFiveWithADoomCounter() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card b = baron(game, me);
        AssertJUnit.assertEquals(1, b.getCounters(CounterEnumType.DOOM));
        AssertJUnit.assertEquals(Integer.valueOf(5), b.getChosenNumber());
        AssertJUnit.assertEquals("doom counter on", b.getView().getChosenNumberLabel());
    }

    @Test
    public void testNumerals() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        // Lightning Bolt: {R}, "deals 3 damage" - a 3 in the text box only
        Card bolt = addCardToZone("Lightning Bolt", me, ZoneType.Hand);
        AssertJUnit.assertTrue(CardFactoryUtil.hasNumeral(bolt, '3'));
        AssertJUnit.assertFalse(CardFactoryUtil.hasNumeral(bolt, '5'));
        // Grizzly Bears: {1}{G}, 2/2 - mana cost and power/toughness
        Card bears = addCardToZone("Grizzly Bears", me, ZoneType.Hand);
        AssertJUnit.assertTrue(CardFactoryUtil.hasNumeral(bears, '1'));
        AssertJUnit.assertTrue(CardFactoryUtil.hasNumeral(bears, '2'));
        AssertJUnit.assertFalse(CardFactoryUtil.hasNumeral(bears, '3'));
    }

    @Test
    public void testCountsDownAndDestroysAnOpponentFromOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card b = baron(game, ai);
        b.setChosenNumber(2);
        // Grizzly Bears has a 2 (2/2): the counter moves from 2 to 1 - not "from 1"
        cast(game, ai, "Grizzly Bears");
        AssertJUnit.assertEquals(Integer.valueOf(1), b.getChosenNumber());
        AssertJUnit.assertFalse(opp.hasLost());
        // and its {1} moves it from 1: the AI destroys its opponent, and the counter goes back on 5
        cast(game, ai, "Grizzly Bears");
        AssertJUnit.assertTrue(opp.hasLost());
        AssertJUnit.assertFalse(ai.hasLost());
    }

    @Test
    public void testNoMatchNoMove() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        Card b = baron(game, me);
        // on 5; Grizzly Bears has no 5 anywhere
        cast(game, me, "Grizzly Bears");
        AssertJUnit.assertEquals(Integer.valueOf(5), b.getChosenNumber());
    }
}
