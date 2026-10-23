package forge.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRarity;
import forge.item.BoosterSlot;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.SealedTemplateWithSlots;
import forge.item.generation.BoosterGenerator;
import forge.model.FModel;

/** Reality Fracture play boosters deal one whole echoed pair plus one card from another pair (2026-10-23). */
public class RealityFractureEchoedPairsTest extends AITest {

    @Test
    public void testEveryPlayBoosterHasOneWholePairAndOneOtherCard() {
        SealedTemplate template = FModel.getMagicDb().getBoosters().get("FRA");
        BoosterSlot slot = ((SealedTemplateWithSlots) template).getNamedSlots().get("EchoedPair");
        Set<String> seen = new HashSet<>();
        int borderlessPairs = 0;
        int rarePairs = 0;
        for (int i = 0; i < 3000; i++) {
            List<PaperCard> echoed = new ArrayList<>();
            for (PaperCard pc : BoosterGenerator.getBoosterPack(template)) {
                // the foil wildcard can also be an echoed pairs card, but never as part of a pair
                if (!pc.isFoil() && slot.getPartner(pc.getName()) != null) {
                    echoed.add(pc);
                }
            }
            AssertJUnit.assertEquals("echoed pairs cards in a pack", 3, echoed.size());
            PaperCard a = echoed.get(0), b = echoed.get(1), c = echoed.get(2);
            AssertJUnit.assertEquals(a.getName() + " dealt without its partner", slot.getPartner(a.getName()), b.getName());
            AssertJUnit.assertEquals(a.getRarity(), b.getRarity());
            AssertJUnit.assertEquals("a pair shares its treatment", isBorderless(a), isBorderless(b));
            AssertJUnit.assertFalse("third card is from another pair",
                    c.getName().equals(a.getName()) || c.getName().equals(b.getName()));
            if (isBorderless(a)) {
                borderlessPairs++;
            }
            if (a.getRarity() != CardRarity.Uncommon) {
                rarePairs++;
            }
            for (PaperCard pc : echoed) {
                seen.add(pc.getName());
            }
        }
        AssertJUnit.assertTrue("borderless pairs turn up", borderlessPairs > 0);
        AssertJUnit.assertTrue("rare and mythic pairs turn up", rarePairs > 0);
        // the sheet spelled these two wrong, so they never came out of it
        AssertJUnit.assertTrue(seen.contains("Lyra, Tolarian Archangel"));
        AssertJUnit.assertTrue(seen.contains("Winter, Tormented Loner"));
        AssertJUnit.assertEquals("all 86 echoed pairs cards are reachable", 86, seen.size());
    }

    private static boolean isBorderless(final PaperCard pc) {
        return Integer.parseInt(pc.getCollectorNumber()) >= 291;
    }
}
