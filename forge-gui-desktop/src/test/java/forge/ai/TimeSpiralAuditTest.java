package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Time Spiral script review and R&D's Secret Lair printed text (2026-10-23). */
public class TimeSpiralAuditTest extends AITest {

    private Card scionWithCribSwapLibrary(final Game game, final Player p) {
        Card scion = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Scion of the Ur-Dragon", "TSP"), p);
        p.getZone(ZoneType.Battlefield).add(scion);
        addCardToZone("Crib Swap", p, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : scion.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.isActivatedAbility() && sa.getRestrictions().checkOtherRestrictions(scion, sa, p)) {
                AbilityUtils.resolve(sa);
                break;
            }
        }
        game.getAction().checkStateEffects(true);
        return game.getCardState(scion);
    }

    @Test
    public void testScionFindsOnlyDragonPermanents() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card scion = scionWithCribSwapLibrary(game, p);
        AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals("Scion of the Ur-Dragon", scion.getName());
    }

    @Test
    public void testScionFindsAnyDragonCardAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card scion = scionWithCribSwapLibrary(game, p);
        AssertJUnit.assertEquals(0, p.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals("Crib Swap", scion.getName());
        AssertJUnit.assertTrue(scion.isInPlay());
    }
}
