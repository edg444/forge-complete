package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** S.N.E.A.K. Dispatcher: an Agents of S.N.E.A.K. card from any library may go into your hand, still its owner's. */
public class SneakDispatcherTest extends AITest {

    private Card onTop(String name, Player p) {
        Card c = createCard(name, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Library).add(c, 0);
        return c;
    }

    private SpellAbility dispatch(Player me, Player target) {
        Card dispatcher = addCard("S.N.E.A.K. Dispatcher", me);
        SpellAbility sa = null;
        for (SpellAbility a : dispatcher.getSpellAbilities()) {
            if (a.getApi() == ApiType.Dig) {
                sa = a;
            }
        }
        sa.setActivatingPlayer(me);
        sa.getTargets().add(target);
        return sa;
    }

    @Test
    public void testSneakCardComesToYourHandStillTheirs() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 3);
        Card detective = onTop("Defective Detective", opp);
        AssertJUnit.assertTrue(detective.hasProperty("Watermark_agentsofsneak", me, detective, null));
        AssertJUnit.assertEquals(detective, opp.getCardsIn(ZoneType.Library).getFirst());

        AbilityUtils.resolve(dispatch(me, opp));
        Card taken = game.getCardState(detective);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Hand).contains(taken));
        AssertJUnit.assertEquals(opp, taken.getOwner());
        AssertJUnit.assertEquals(me, taken.getController());
    }

    @Test
    public void testAnythingElseStaysInItsOwnersLibrary() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 3);
        Card bears = onTop("Grizzly Bears", opp);
        AssertJUnit.assertEquals(bears, opp.getCardsIn(ZoneType.Library).getFirst());

        AbilityUtils.resolve(dispatch(me, opp));
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Library).contains(game.getCardState(bears)));
        AssertJUnit.assertEquals(4, opp.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Hand).size());
    }
}
