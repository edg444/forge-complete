package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRarity;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.BoosterGenerator;
import forge.model.FModel;

/** Battlebond packs never split a "partner with" pair; rates from the lethe.xyz collation project (2026-10-23). */
public class BattlebondPartnersTest extends AITest {

    @Test
    public void testPartnersAlwaysOpenTogether() {
        SealedTemplate template = FModel.getMagicDb().getBoosters().get("BBD");
        int packs = 20000, rarePairs = 0, uncommonPairs = 0, mythics = 0;
        for (int i = 0; i < packs; i++) {
            List<PaperCard> pack = BoosterGenerator.getBoosterPack(template);
            AssertJUnit.assertEquals("cards in a pack", 15, pack.size());
            boolean rarePair = false, uncommonPair = false, mythic = false;
            int uncommons = 0;
            for (int j = 0; j < pack.size(); j++) {
                PaperCard pc = pack.get(j);
                if (!pc.isFoil() && pc.getRarity() == CardRarity.Uncommon) {
                    uncommons++;
                }
                if (!pc.isFoil() && pc.getRarity() == CardRarity.MythicRare) {
                    mythic = true;
                }
                String partner = pc.getRules().getPartnerWith();
                if (partner.isEmpty()) {
                    continue;
                }
                PaperCard before = j > 0 ? pack.get(j - 1) : null, after = j + 1 < pack.size() ? pack.get(j + 1) : null;
                boolean nextToPartner = (before != null && before.getName().equals(partner) && before.isFoil() == pc.isFoil())
                        || (after != null && after.getName().equals(partner) && after.isFoil() == pc.isFoil());
                AssertJUnit.assertTrue(pc.getName() + " opened without " + partner + ": " + pack, nextToPartner);
                if (!pc.isFoil() && pc.getRarity() == CardRarity.Uncommon) {
                    uncommonPair = true;
                } else if (!pc.isFoil()) {
                    rarePair = true;
                }
            }
            AssertJUnit.assertFalse("a rare pair and an uncommon pair in one pack: " + pack, rarePair && uncommonPair);
            AssertJUnit.assertEquals("uncommons in a pack: " + pack, rarePair ? 2 : 3, uncommons);
            if (rarePair) {
                rarePairs++;
            }
            if (uncommonPair) {
                uncommonPairs++;
            }
            if (mythic) {
                mythics++;
            }
        }
        assertRate("rare or mythic pair", 1.0 / 10, rarePairs, packs, 0.01);
        assertRate("uncommon pair", 29.0 / 139, uncommonPairs, packs, 0.015);
        assertRate("mythic pack", 7.0 / 55, mythics, packs, 0.015);
    }

    private static void assertRate(final String what, final double expected, final int count, final int packs, final double tolerance) {
        double rate = (double) count / packs;
        AssertJUnit.assertTrue(what + " rate " + rate + ", expected about " + expected, Math.abs(rate - expected) < tolerance);
    }
}
