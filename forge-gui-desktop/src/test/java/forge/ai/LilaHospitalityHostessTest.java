package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Lila, Hospitality Hostess (UNF #170), checked against Scryfall and its rulings 2026-10-21: common spells castable from
 * the top of the library, by the printing's rarity.
 */
public class LilaHospitalityHostessTest extends AITest {

    private Card onTop(final String name, final String set, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set);
        final Card c = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.Library).add(c, 0);
        return c;
    }

    private boolean castable(final Card c, final Player p) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.isSpell()) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testCommonSpellsFromTheTop() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Lila, Hospitality Hostess", p);
        for (int i = 0; i < 6; i++) {
            addCard("Forest", p);
        }
        final Card elves = onTop("Llanowar Elves", "M19", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(forge.card.CardRarity.Common, elves.getRarity());
        AssertJUnit.assertTrue("a common on top", castable(elves, p));

        final Card baneslayer = onTop("Baneslayer Angel", "M10", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("a mythic on top", castable(baneslayer, p));
    }
}
