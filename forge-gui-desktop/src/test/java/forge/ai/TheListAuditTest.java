package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.card.CardEdition;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * The List (PLST) and The List (Unfinity Foil Edition) (ULST) against Scryfall, 2026-10-20: PLST matched by collector
 * number except three Judgment † variations; ULST's 62 printings were missing entirely. ULST's two Ineffable Blessings
 * and two Everythingamajigs are matched to their Unstable variants by Oracle text.
 */
public class TheListAuditTest extends AITest {

    private Card printing(final String name, final String set, final String number, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, number);
        AssertJUnit.assertNotNull(name + " (" + set + " " + number + ")", pc);
        return Card.fromPaperCard(pc, p);
    }

    @Test
    public void testEveryUnfinityFoilPrintingLoads() {
        final CardEdition ulst = StaticData.instance().getEditions().get("ULST");
        AssertJUnit.assertNotNull(ulst);
        AssertJUnit.assertEquals(62, ulst.getCards().size());
        for (final CardEdition.EditionEntry e : ulst.getCards()) {
            AssertJUnit.assertNotNull(e.name() + " " + e.collectorNumber(),
                    FModel.getMagicDb().getCommonCards().getCard(e.name(), "ULST", e.collectorNumber()));
        }
    }

    @Test
    public void testUnfinityFoilVariantsAndBorder() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(printing("Ineffable Blessing", "ULST", "37", p).getOracleText().contains("white-bordered or silver-bordered"));
        AssertJUnit.assertTrue(printing("Ineffable Blessing", "ULST", "38", p).getOracleText().contains("Flavorful or Bland"));
        AssertJUnit.assertTrue(printing("Everythingamajig", "ULST", "55", p).getOracleText().contains("Move a counter"));
        AssertJUnit.assertTrue(printing("Everythingamajig", "ULST", "56", p).getOracleText().contains("Scry 2"));
        AssertJUnit.assertTrue(printing("Earl of Squirrel", "ULST", "108", p).isSilverBorderedOrAcorn());
    }

    @Test
    public void testJudgmentVariationsLoad() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        printing("Arcane Teachings", "PLST", "JUD-78†", p);
        printing("Grizzly Fate", "PLST", "JUD-119†", p);
        printing("Phantom Centaur", "PLST", "JUD-127†", p);
    }
}
