package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Secret Base (Unstable, 165a-e) - Oracle text and each printing's watermark checked against Scryfall 2026-10-09
 * (no rulings).
 */
public class SecretBaseTest extends AITest {

    private Card printed(final String name, final String set, final int art, final Player p, final ZoneType zone) {
        final PaperCard pc = art > 0 ? FModel.getMagicDb().getCommonCards().getCard(name, set, art)
                : FModel.getMagicDb().getCommonCards().getCard(name, set);
        final Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    private SpellAbility restrictedMana(final Card base) {
        for (final SpellAbility sa : base.getManaAbilities()) {
            if (!sa.getManaPart().getManaRestrictions().isEmpty()) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void eachPrintingHasItsFactionsWatermark() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final String[] expected = {"orderofthewidget", "agentsofsneak", "leagueofdastardlydoom", "goblinexplosioneers",
                "crossbreedlabs"};
        for (int art = 1; art <= 5; art++) {
            final Card base = printed("Secret Base", "UST", art, me, ZoneType.Battlefield);
            AssertJUnit.assertEquals(expected[art - 1], base.getWatermark());
        }
    }

    @Test
    public void anyColorOnlyForASpellSharingItsWatermark() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card base = printed("Secret Base", "UST", 4, me, ZoneType.Battlefield);   // Goblin Explosioneers
        AssertJUnit.assertEquals("goblinexplosioneers", base.getWatermark());
        final SpellAbility anyColor = restrictedMana(base);
        AssertJUnit.assertNotNull(anyColor);

        final Card goblin = printed("Goblin Haberdasher", "UST", 0, me, ZoneType.Hand);       // Goblin Explosioneers
        final Card druid = printed("Druid of the Sacred Beaker", "UST", 0, me, ZoneType.Hand); // Crossbreed Labs
        final Card bear = addCardToZone("Runeclaw Bear", me, ZoneType.Hand);                  // none
        for (final Card c : new Card[] {goblin, druid, bear}) {
            c.getFirstSpellAbility().setActivatingPlayer(me);
        }
        AssertJUnit.assertTrue(anyColor.getManaPart().meetsManaRestrictions(goblin.getFirstSpellAbility()));
        AssertJUnit.assertFalse(anyColor.getManaPart().meetsManaRestrictions(druid.getFirstSpellAbility()));
        AssertJUnit.assertFalse(anyColor.getManaPart().meetsManaRestrictions(bear.getFirstSpellAbility()));
    }
}
