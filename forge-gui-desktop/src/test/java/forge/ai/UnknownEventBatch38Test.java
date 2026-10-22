package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Minnea, Planar Tourist, The Generational Historian, The Strixhaven-Lorwyn Rover and The Waffle Restaurant (UNK
 * mythics), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch38Test extends AITest {

    private static PaperCard paper(final String name, final String set, final String number) {
        return FModel.getMagicDb().getCommonCards().getCard(name, set, number);
    }

    private static Card printing(final String name, final String set, final String number, final Player owner) {
        final Card c = Card.fromPaperCard(paper(name, set, number), owner);
        AssertJUnit.assertNotNull(name, c);
        return c;
    }

    private static int costOf(final Card c, final Player p) {
        final SpellAbility cast = c.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(cast.getPayCosts().getTotalMana());
        forge.game.cost.CostAdjustment.adjust(cost, cast, p, null, true, false);
        return cost.getConvertedManaCost();
    }

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testMinneaPlane() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card minnea = addCard("Minnea, Planar Tourist", ai);
        minnea.setChosenType("Mirrodin");
        final Card scout = printing("Viridian Scout", "5DN", "100", ai);
        ai.getZone(ZoneType.Hand).add(scout);
        AssertJUnit.assertTrue(scout.isValid("Card.fromChosenPlane", ai, minnea, null));
        AssertJUnit.assertEquals("{3}{G} less one colored symbol", 3, costOf(scout, ai));
        minnea.setChosenType("Kamigawa");
        AssertJUnit.assertEquals(4, costOf(scout, ai));
    }

    @Test
    public void testHistorianYear() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card historian = addCard("The Generational Historian", ai);
        historian.setChosenNumber(1994);
        final Card bears = printing("Grizzly Bears", "3ED", "201", ai);
        ai.getZone(ZoneType.Hand).add(bears);
        AssertJUnit.assertEquals("1994: {1} and {G} less", 0, costOf(bears, ai));
        historian.setChosenNumber(2020);
        AssertJUnit.assertEquals(2, costOf(bears, ai));
    }

    @Test
    public void testRoverCollege() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rover = addCardToZone("The Strixhaven-Lorwyn Rover", ai, ZoneType.Command);
        ai.addCommander(rover);
        rover.setCommander(true);
        addCardToZone("Lightning Helix", ai, ZoneType.Library);
        addCardToZone("Goblin Piker", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertTrue("Lorehold fits a red-white deck", rover.hasChosenColor("red") && rover.hasChosenColor("white"));
        AssertJUnit.assertEquals("Goblin", rover.getChosenType());
        AssertJUnit.assertTrue(ai.getCommanderColorID().hasRed() && ai.getCommanderColorID().hasWhite());
        final Card helix = addCardToZone("Lightning Helix", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(helix.isValid("Instant.allChosenColors", ai, rover, null));
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(bolt.isValid("Instant.allChosenColors", ai, rover, null));
    }

    @Test
    public void testCommanderDeckRules() {
        initAndCreateGame();
        final Deck rover = new Deck();
        rover.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("The Strixhaven-Lorwyn Rover"));
        rover.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Lightning Helix"));
        AssertJUnit.assertNull(DeckFormat.Commander.getCommanderConformanceProblem(rover));
        rover.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Counterspell"));
        AssertJUnit.assertNotNull("no college has blue, red and white", DeckFormat.Commander.getCommanderConformanceProblem(rover));

        final Deck waffle = new Deck();
        waffle.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("The Waffle Restaurant"));
        waffle.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Bake into a Pie"));
        waffle.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Forest"));
        AssertJUnit.assertNull(DeckFormat.Commander.getCommanderConformanceProblem(waffle), DeckFormat.Commander.getCommanderConformanceProblem(waffle));
        waffle.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Counterspell"));
        AssertJUnit.assertNotNull(DeckFormat.Commander.getCommanderConformanceProblem(waffle));
    }
}
