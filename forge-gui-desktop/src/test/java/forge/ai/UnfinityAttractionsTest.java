package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Centrifuge (UNF #203), Cover the Spot (UNF #207), Dart Throw (UNF #208) and Gift Shop (UNF #214), checked against
 * Scryfall and their rulings 2026-10-21.
 */
public class UnfinityAttractionsTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void visit(final Card attraction, final String svar, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(attraction.getSVar(svar), attraction);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        resolveAll(p.getGame());
    }

    /** Attractions are kept with the other variant cards. */
    private Card attraction(final String name, final Player p) {
        final Card c = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard(name), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private int named(final Player p, final String type) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype(type)
                || c.getType().hasCreatureType(type)).size();
    }

    @Test
    public void testCentrifuge() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Player other = game.getPlayers().get(0);
        addCardToZone("Island", other, ZoneType.Library);
        addCardToZone("Forest", me, ZoneType.Library);
        final Card centrifuge = attraction("Centrifuge", me);
        visit(centrifuge, "TrigDraw", me);
        final Card drawn = me.getCardsIn(ZoneType.Hand).getFirst();
        AssertJUnit.assertEquals("from the library to my right", "Island", drawn.getName());
        AssertJUnit.assertEquals("still its owner's", other, drawn.getOwner());
        AssertJUnit.assertEquals("Forest", other.getCardsIn(ZoneType.Hand).getFirst().getName());
        AssertJUnit.assertEquals(3, named(me, "Treasure"));
    }

    @Test
    public void testDartThrow() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card dart = attraction("Dart Throw", me);
        visit(dart, "TrigThrow", me);
        final boolean hit = dart.getSVar("Hits").equals("Number$1");
        AssertJUnit.assertEquals(hit ? 2 : 0, named(me, "Teddy Bear"));
        AssertJUnit.assertEquals("sacrificed after its prize", !hit, dart.isInPlay());
    }

    @Test
    public void testCoverTheSpot() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card spot = attraction("Cover the Spot", me);
        visit(spot, "TrigDrop", me);
        final boolean covered = spot.getSVar("Covered").equals("Number$1");
        final int hits = Integer.parseInt(spot.getSVar("Hits").replace("Number$", ""));
        AssertJUnit.assertTrue("covering takes all three cards; a miss never has all three", covered == (hits == 3));
        AssertJUnit.assertEquals(!covered, spot.isInPlay());
    }

    @Test
    public void testGiftShopLoads() {
        final Game game = initAndCreateGame();
        final Card shop = attraction("Gift Shop", game.getPlayers().get(1));
        AssertJUnit.assertTrue(shop.getSVar("TrigCharm").contains("ChoiceRestriction$ ThisGame"));
    }
}
