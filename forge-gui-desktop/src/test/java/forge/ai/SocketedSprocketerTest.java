package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Socketed Sprocketer: die results installed on it are kept for later rolls, or a 6 for a card. */
public class SocketedSprocketerTest extends AITest {

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testRollReplacesWhateverWasInstalled() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card sprocketer = addCard("Socketed Sprocketer", me);
        sprocketer.installResult(3);
        sprocketer.installResult(4);

        SpellAbility install = ability(sprocketer, ApiType.InstallResult);
        install.setActivatingPlayer(me);
        AbilityUtils.resolve(install);
        AssertJUnit.assertEquals(1, sprocketer.getInstalledResults().size());
        int result = sprocketer.getInstalledResults().get(0);
        AssertJUnit.assertTrue(result >= 1 && result <= 6);
        AssertJUnit.assertEquals(String.valueOf(result), sprocketer.getView().getInstalledResults());
    }

    @Test
    public void testUninstallASixToDraw() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card sprocketer = addCard("Socketed Sprocketer", me);
        SpellAbility draw = ability(sprocketer, ApiType.Draw);
        draw.setActivatingPlayer(me);
        sprocketer.installResult(5);
        AssertJUnit.assertFalse(draw.getPayCosts().canPay(draw, me, false));
        sprocketer.installResult(6);
        AssertJUnit.assertTrue(draw.getPayCosts().canPay(draw, me, false));
        AssertJUnit.assertTrue(draw.getDescription(), draw.getDescription().startsWith("Uninstall a 6 from this creature: Draw a card."));
    }

    @Test
    public void testInstalledResultStandsInForARoll() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card sprocketer = addCard("Socketed Sprocketer", me);
        int swaps = 0;
        for (int i = 0; i < 40; i++) {
            sprocketer.clearInstalledResults();
            sprocketer.installResult(5);
            int roll = RollDiceEffect.rollDiceForPlayer(null, me, 1, 6);
            // the AI swaps a lower roll for the 5 and keeps a 5 or 6
            AssertJUnit.assertTrue("rolled " + roll, roll >= 5);
            if (sprocketer.getInstalledResults().isEmpty()) {
                swaps++;
            }
        }
        AssertJUnit.assertTrue(swaps > 0);
    }

    @Test
    public void testANewObjectHasNoDice() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card sprocketer = addCard("Socketed Sprocketer", me);
        sprocketer.installResult(6);
        Card bounced = game.getAction().moveTo(ZoneType.Hand, sprocketer, null, null);
        AssertJUnit.assertTrue(bounced.getInstalledResults().isEmpty());
    }
}
