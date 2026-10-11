package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Squidnapper (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: it takes a
 * creature until it leaves the battlefield or that creature's player pays the ransom ({6} and 2 life), a special
 * action they may take any time they have priority.
 */
public class SquidnapperTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private static SpellAbility ransom(final Card c, final Player p) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.getApi() == ApiType.Ransom) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testRansomBuysItBack() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card angel = addCard("Serra Angel", opp);
        addCards("Island", 6, opp);
        Card squid = addCardToZone("Squidnapper", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(squid.getAbilityText(), squid.getAbilityText().contains("Ransom — {6} and 2 life"));

        squid = game.getAction().moveToPlay(squid, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        resolveAll(game);
        AssertJUnit.assertEquals(ai, angel.getController());

        // only the player it was taken from may pay
        AssertJUnit.assertNull(ransom(squid, ai));
        SpellAbility pay = ransom(squid, opp);
        AssertJUnit.assertNotNull(pay);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(opp, pay, null));
        AssertJUnit.assertEquals(opp, angel.getController());
        AssertJUnit.assertEquals(18, opp.getLife());
        AssertJUnit.assertNull(ransom(squid, opp));
    }

    @Test
    public void testLeavingGivesItBack() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card angel = addCard("Serra Angel", opp);
        Card squid = game.getAction().moveToPlay(addCardToZone("Squidnapper", ai, ZoneType.Hand), null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        resolveAll(game);
        AssertJUnit.assertEquals(ai, angel.getController());

        game.getAction().destroy(squid, null, false, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(opp, angel.getController());
    }
}
