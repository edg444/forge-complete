package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.zone.ZoneType;

/**
 * Priority Avenger (Mystery Booster playtest card): "Players can't cast instant spells unless a spell or ability is on
 * the stack." - Oracle text and rulings checked against Scryfall 2026-10-10: abilities on the stack count; non-instant
 * spells with flash aren't affected.
 */
public class PriorityAvengerTest extends AITest {

    private static boolean cantCast(Card c, Player p) {
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return StaticAbilityCantBeCast.cantBeCastAbility(sa, c, p);
    }

    @Test
    public void testInstantsWaitForTheStack() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Priority Avenger", me);
        Card shade = addCard("Frozen Shade", opp);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        Card mine = addCardToZone("Shock", me, ZoneType.Hand);
        Card flash = addCardToZone("Ambush Viper", opp, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(cantCast(shock, opp));
        AssertJUnit.assertTrue(cantCast(mine, me));
        AssertJUnit.assertFalse(cantCast(flash, opp));

        // an activated ability on the stack opens the window
        SpellAbility pump = shade.getSpellAbilities().getFirst();
        pump.setActivatingPlayer(opp);
        game.getStack().add(pump);
        AssertJUnit.assertFalse(cantCast(shock, opp));
        AssertJUnit.assertFalse(cantCast(mine, me));
    }
}
