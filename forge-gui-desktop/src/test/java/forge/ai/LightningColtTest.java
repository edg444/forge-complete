package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Lightning Colt (CMB1 #57), an Instant Creature checked against Scryfall 2026-10-13 (no rulings): cast any time you
 * could cast an instant, it enters the battlefield (user ruling for Visitor from Planet Q) and its trigger deals 3.
 */
public class LightningColtTest extends AITest {

    @Test
    public void castAtInstantSpeedEntersAndDealsThree() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Mountain", 5, ai);
        Card bears = addCard("Grizzly Bears", opp);
        Card colt = addCardToZone("Lightning Colt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = colt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sa.canCastTiming(ai));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());

        Card onField = game.getCardState(colt);
        AssertJUnit.assertTrue(onField.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(3, onField.getNetPower());
        AssertJUnit.assertTrue("3 damage somewhere",
                bears.isInZone(ZoneType.Graveyard) || opp.getLife() == 17);
    }
}
