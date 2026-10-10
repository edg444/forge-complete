package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.PrintingTraits;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Stamp of Approval (Unstable) - Oracle text checked against Scryfall 2026-10-09 (no rulings). Watermarks are the
 * printings' (PrintingTraits); "Set symbol" stands for every set-symbol watermark (user, 2026-10-09).
 */
public class StampOfApprovalTest extends AITest {

    private Card printed(final String name, final String set, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set);
        final Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    @Test
    public void creaturesWithTheChosenWatermarkGetPlusOne() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card druid = printed("Druid of the Sacred Beaker", "UST", me);    // Crossbreed Labs
        final Card rigger = printed("Wrench-Rigger", "UST", me);                // Goblin Explosioneers
        final Card theirs = printed("Hydradoodle", "UST", opp);                 // Crossbreed Labs, but not mine
        AssertJUnit.assertEquals("crossbreedlabs", druid.getWatermark());

        final Card stamp = addCard("Stamp of Approval", me);
        stamp.setChosenType("Crossbreed Labs");
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(druid.getBasePower() + 1, druid.getNetPower());
        AssertJUnit.assertEquals(druid.getBaseToughness() + 1, druid.getNetToughness());
        AssertJUnit.assertEquals(rigger.getBasePower(), rigger.getNetPower());
        AssertJUnit.assertEquals(theirs.getBasePower(), theirs.getNetPower());
    }

    @Test
    public void everyWatermarkIsAChoice() {
        initAndCreateGame();
        final List<String> all = PrintingTraits.allWatermarkNames();
        AssertJUnit.assertTrue(all.contains("Goblin Explosioneers"));
        AssertJUnit.assertTrue(all.contains("Agents of S.N.E.A.K."));
        AssertJUnit.assertTrue(all.contains("Selesnya"));
        AssertJUnit.assertTrue(all.contains("Set symbol"));
        AssertJUnit.assertEquals("set", PrintingTraits.watermarkId("Set symbol"));
        AssertJUnit.assertEquals("agentsofsneak", PrintingTraits.watermarkId("Agents of S.N.E.A.K."));
        AssertJUnit.assertEquals("selesnya", PrintingTraits.watermarkId("Selesnya"));
    }

    @Test
    public void aiChoosesItsCreaturesWatermark() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        printed("Druid of the Sacred Beaker", "UST", me);
        printed("Slaying Mantis", "UST", me);
        printed("Wrench-Rigger", "UST", me);
        final Card stamp = addCardToZone("Stamp of Approval", me, ZoneType.Hand);
        final Card played = game.getAction().moveToPlay(stamp, null, null);
        AssertJUnit.assertEquals("Crossbreed Labs", played.getChosenType());
    }
}
