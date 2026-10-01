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
 * Subcontract (Unstable): "A person outside the game looks at target opponent's hand and chooses a nonland card from
 * it. That player discards that card." - Oracle text and rulings checked against Scryfall 2026-10-01. The person is
 * simulated: a random nonland card, likelier the more impactful it is.
 */
public class SubcontractTest extends AITest {

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card sub = addCardToZone("Subcontract", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertEquals("A person outside the game looks at target opponent's hand and chooses a nonland card "
                + "from it. That player discards that card.", sub.getAbilityText().trim());
    }

    @Test
    public void testNonlandWeightedByImpact() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        int dragon = 0;
        int bears = 0;
        for (int i = 0; i < 200; i++) {
            opp.getZone(ZoneType.Hand).removeAllCards(true);
            addCardToZone("Mountain", opp, ZoneType.Hand);
            addCardToZone("Forest", opp, ZoneType.Hand);
            addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
            addCardToZone("Shivan Dragon", opp, ZoneType.Hand);

            SpellAbility sa = addCardToZone("Subcontract", me, ZoneType.Hand).getFirstSpellAbility();
            sa.setActivatingPlayer(me);
            sa.getTargets().add(opp);
            AbilityUtils.resolve(sa);

            AssertJUnit.assertEquals(3, opp.getCardsIn(ZoneType.Hand).size());
            Card discarded = opp.getCardsIn(ZoneType.Graveyard).getLast();
            AssertJUnit.assertFalse(discarded.isLand());
            if (discarded.getName().equals("Shivan Dragon")) {
                dragon++;
            } else {
                bears++;
            }
        }
        AssertJUnit.assertEquals(200, dragon + bears);
        AssertJUnit.assertTrue("dragon " + dragon + " bears " + bears, dragon > bears && bears > 0);
    }
}
