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
 * Super-Duper Death Ray (Unstable): "Trample (This spell can deal excess damage to its target's controller.) / Super-Duper
 * Death Ray deals 4 damage to target creature." - Oracle text and the four rulings checked 2026-10-02: a 1/1 takes 1
 * and its controller 3; damage already marked counts; only the creature is targeted, so a hexproof player still
 * takes the excess.
 */
public class SuperDuperDeathRayTest extends AITest {

    private void cast(Player caster, Card target) {
        Card ray = addCardToZone("Super-Duper Death Ray", caster, ZoneType.Hand);
        SpellAbility sa = ray.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        caster.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card ray = addCardToZone("Super-Duper Death Ray", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertEquals("Trample (This spell can deal excess damage to its target's controller.) | "
                + "Super-Duper Death Ray deals 4 damage to target creature.",
                String.join(" | ", ray.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testExcessGoesToTheController() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card elves = addCard("Llanowar Elves", opp);
        cast(me, elves);
        AssertJUnit.assertTrue(game.getCardState(elves).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(17, opp.getLife());
        AssertJUnit.assertEquals(20, me.getLife());
    }

    @Test
    public void testMarkedDamageCounts() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card dreadmaw = addCard("Colossal Dreadmaw", opp);
        cast(me, dreadmaw);
        AssertJUnit.assertEquals(4, dreadmaw.getDamage());
        AssertJUnit.assertEquals(20, opp.getLife());
        // the second one never sees it coming: 2 to the 6/6, 2 to its controller
        cast(me, dreadmaw);
        AssertJUnit.assertTrue(game.getCardState(dreadmaw).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(18, opp.getLife());
    }

    @Test
    public void testHexproofPlayerStillTakesTheExcess() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Leyline of Sanctity", opp);
        Card elves = addCard("Llanowar Elves", opp);
        game.getAction().checkStateEffects(true);
        cast(me, elves);
        AssertJUnit.assertEquals(17, opp.getLife());
    }
}
