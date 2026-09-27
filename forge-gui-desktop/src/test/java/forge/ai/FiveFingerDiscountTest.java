package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Five-Finger Discount: the permanent goes into the caster's hand without changing its owner (Unstable rulings).
 */
public class FiveFingerDiscountTest extends AITest {

    private Card steal(Player me, Card target) {
        Card ffd = addCardToZone("Five-Finger Discount", me, ZoneType.Hand);
        SpellAbility sa = ffd.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        return me.getGame().getCardState(target);
    }

    @Test
    public void testIntoYourHandStillTheirs() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card shivan = steal(me, addCard("Shivan Dragon", opp));

        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(shivan));
        AssertJUnit.assertEquals(opp, shivan.getOwner());
        AssertJUnit.assertEquals(me, shivan.getController());
        // yours to cast (it's your main phase), not its owner's
        SpellAbility cast = shivan.getFirstSpellAbility();
        cast.setActivatingPlayer(me);
        AssertJUnit.assertTrue(cast.getRestrictions().canPlay(shivan, cast));
        cast.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(cast.getRestrictions().canPlay(shivan, cast));
    }

    @Test
    public void testLeavingYourHandGoesToItsOwner() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card shivan = steal(me, addCard("Shivan Dragon", opp));

        Card discarded = game.getAction().moveToGraveyard(shivan, null, null);
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Graveyard).contains(discarded));
        AssertJUnit.assertEquals(opp, discarded.getController());

        Card bears = steal(me, addCard("Grizzly Bears", opp));
        Card shuffled = game.getAction().moveToLibrary(bears, 0, null, null);
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Library).contains(shuffled));
    }

    @Test
    public void testAiCastsItWithOffColorMana() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 5);
        fillLibrary(opp, 5);
        for (int i = 0; i < 6; i++) {
            addCard("Island", me);
        }
        Card shivan = steal(me, addCard("Shivan Dragon", opp));

        // {4}{R}{R} paid with Islands only: the next cast may spend mana as though it were any color
        for (int i = 0; i < 200 && !game.getCardState(shivan).isInPlay(); i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        Card inPlay = game.getCardState(shivan);
        AssertJUnit.assertTrue(inPlay.isInPlay());
        AssertJUnit.assertEquals(me, inPlay.getController());
        AssertJUnit.assertEquals(opp, inPlay.getOwner());
        // "the next time you cast that card" - used up
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Command).stream().noneMatch(c -> c.isRemembered(inPlay)));
    }
}
