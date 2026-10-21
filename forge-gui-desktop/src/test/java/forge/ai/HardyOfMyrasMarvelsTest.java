package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Hardy of Myra's Marvels (UNF #141), checked against Scryfall and its rulings 2026-10-21. Flavor lines are the
 * printing's, read off its card image: M12 Llanowar Elves has 3, Secret Lair 1553 has 2, and 30A 206 has no flavor text.
 */
public class HardyOfMyrasMarvelsTest extends AITest {

    private Card elves(final String set, final String cn, final Player p, final ZoneType zone) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves", set, cn);
        final Card c = Card.fromPaperCard(pc, p);
        p.getZone(zone).add(c);
        return c;
    }

    private Card elves(final String set, final String cn, final Player p) {
        return elves(set, cn, p, ZoneType.Hand);
    }

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Player p, final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, sa, null));
        resolveAll(p.getGame());
    }

    @Test
    public void testPrintedFlavorLines() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertEquals(3, elves("M12", "182", p).getPrintedFlavorLines());
        AssertJUnit.assertEquals(2, elves("SLD", "1553", p).getPrintedFlavorLines());
        AssertJUnit.assertEquals(0, elves("30A", "206", p).getPrintedFlavorLines());
    }

    @Test
    public void testCountersForTheChosenNumber() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card hardy = addCard("Hardy of Myra's Marvels", p);
        hardy.setChosenNumber(3);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", p);
        }
        game.getAction().checkStateEffects(true);
        cast(p, elves("SLD", "1553", p));
        AssertJUnit.assertEquals("two lines isn't three", 0, hardy.getCounters(CounterEnumType.P1P1));
        cast(p, elves("M12", "182", p));
        AssertJUnit.assertEquals(3, hardy.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testAiChoosesTheBestPayingNumber() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            elves("M12", "182", ai, ZoneType.Library);
        }
        for (int i = 0; i < 5; i++) {
            elves("30A", "206", ai, ZoneType.Library);
        }
        elves("SLD", "1553", ai, ZoneType.Library);
        final Card hardy = addCardToZone("Hardy of Myra's Marvels", ai, ZoneType.Hand);
        final SpellAbility choose = forge.game.ability.AbilityFactory.getAbility(hardy.getSVar("ChooseNum"), hardy);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(3, ai.getController().chooseNumber(choose, "", 0, 20));
    }
}
