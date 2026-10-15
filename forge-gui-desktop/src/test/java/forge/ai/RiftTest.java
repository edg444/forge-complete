package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.mulligan.LondonMulligan;
import forge.game.mulligan.ParisMulligan;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Rift (CMB1 #119), checked against Scryfall and its 2 rulings 2026-10-15: it may start the game in the opening
 * hand in place of one draw - for the opening hand and each mulligan, which doesn't shuffle it back.
 */
public class RiftTest extends AITest {

    /** The pre-game choice, then the opening hand as GameAction draws it. */
    private Card startWithRift(Game game, Player p) {
        fillLibrary(p, 30);
        Card rift = addCardToZone("Rift", p, ZoneType.Library);
        SpellAbility start = AbilityFactory.getAbility(rift.getSVar("DBStart"), rift);
        start.setActivatingPlayer(p);
        p.getController().playSpellAbilityNoStack(start, true);
        AssertJUnit.assertTrue(game.getCardState(rift).isInZone(ZoneType.Hand));
        p.setOpeningHandStarters(p.getCardsIn(ZoneType.Hand));
        p.drawCards(p.getStartingHandSize() - p.getOpeningHandStartersInHand());
        return game.getCardState(rift);
    }

    @Test
    public void openingHandIsSixPlusRift() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rift = startWithRift(game, ai);
        AssertJUnit.assertEquals(7, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(ai.isOpeningHandStarter(rift));
    }

    @Test
    public void mulligansKeepItAndDrawOneFewer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rift = startWithRift(game, ai);

        ParisMulligan paris = new ParisMulligan(ai, false);
        paris.mulligan();
        AssertJUnit.assertTrue("not shuffled back", rift.isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals("six cards after one Paris mulligan, Rift among them", 6, ai.getCardsIn(ZoneType.Hand).size());

        LondonMulligan london = new LondonMulligan(ai, false);
        london.mulligan();
        AssertJUnit.assertEquals("draw six beside Rift, then put one on the bottom", 6, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void entersTappedForColorless() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rift = addCardToZone("Rift", ai, ZoneType.Hand);
        Card played = game.getAction().moveToPlay(rift, ai, null, AbilityKey.newMap());
        AssertJUnit.assertTrue(played.isTapped());
        AssertJUnit.assertEquals("C", played.getManaAbilities().get(0).getManaPart().getOrigProduced());
    }
}
