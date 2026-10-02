package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Hammerfest Boomtacular (Unstable): "Whenever you cast a spell with a Goblin Explosioneers watermark, this enchantment
 * deals 2 damage to any target." - Oracle text checked against Scryfall 2026-10-02 (no rulings, no FAQ entry). The
 * watermark is the printing's (PrintingTraits); Goblin Haberdasher's UST printing has it.
 */
public class HammerfestBoomtacularTest extends AITest {

    private Card inHand(String name, String set, Player p) {
        PaperCard pc = set == null ? FModel.getMagicDb().getCommonCards().getCard(name)
                : FModel.getMagicDb().getCommonCards().getCard(name, set);
        Card c = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.Hand).add(c);
        return c;
    }

    private int triggersFrom(Game game, Card source) {
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        int n = 0;
        for (SpellAbilityStackInstance si : game.getStack()) {
            if (si.getSourceCard().equals(source) && si.isTrigger()) {
                n++;
            }
        }
        return n;
    }

    private void cast(Game game, Player p, Card c) {
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, sa, null));
    }

    @Test
    public void testWatermarkedSpellTriggers() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card hammerfest = addCard("Hammerfest Boomtacular", me);
        for (int i = 0; i < 3; i++) {
            addCard("Mountain", me);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Whenever you cast a spell with a Goblin Explosioneers watermark, this enchantment deals "
                + "2 damage to any target.", hammerfest.getAbilityText().trim());

        Card haberdasher = inHand("Goblin Haberdasher", "UST", me);
        AssertJUnit.assertEquals("goblinexplosioneers", haberdasher.getWatermark());
        cast(game, me, haberdasher);
        AssertJUnit.assertEquals(1, triggersFrom(game, hammerfest));
    }

    @Test
    public void testOtherSpellsDont() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card hammerfest = addCard("Hammerfest Boomtacular", me);
        for (int i = 0; i < 2; i++) {
            addCard("Forest", me);
        }
        game.getAction().checkStateEffects(true);

        Card bears = inHand("Grizzly Bears", null, me);
        AssertJUnit.assertNull(bears.getWatermark());
        cast(game, me, bears);
        AssertJUnit.assertEquals(0, triggersFrom(game, hammerfest));
    }
}
