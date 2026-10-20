package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Bat- (UND #32), checked against Scryfall and its 2 rulings 2026-10-20. Adorable Kitten is the host: "When this
 * creature enters, roll a six-sided die. You gain life equal to the result."
 */
public class BatAugmentTest extends AITest {

    private Card combined(Game game, Player me) {
        Card kitten = addCard("Adorable Kitten", me);
        Card bat = addCardToZone("Bat-", me, ZoneType.Hand);
        for (SpellAbility sa : bat.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                sa.setActivatingPlayer(me);
                sa.getTargets().add(kitten);
                AbilityUtils.resolve(sa);
            }
        }
        game.getAction().checkStateEffects(true);
        return kitten;
    }

    private void finishTurn(Game game, Player me) {
        int guard = 0;
        while (!game.isGameOver() && game.getPhaseHandler().getPlayerTurn() == me) {
            game.getPhaseHandler().mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 500);
        }
    }

    private void opponentLoses(Game game, Player me, Player opp, int amount) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);
        opp.loseLife(amount, false, false, null);
    }

    @Test
    public void testCombinedCreature() {
        Game game = initAndCreateGame();
        Card c = combined(game, game.getPlayers().get(1));
        AssertJUnit.assertEquals("Bat-Kitten", c.getName());
        AssertJUnit.assertEquals(2, c.getNetPower());
        AssertJUnit.assertEquals(2, c.getNetToughness());
        AssertJUnit.assertTrue(c.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Bat"));
        AssertJUnit.assertTrue(c.getColor().hasBlack());
    }

    @Test
    public void testTwoLifeIsntEnough() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        combined(game, me);
        opponentLoses(game, me, game.getPlayers().get(0), 2);
        finishTurn(game, me);
        AssertJUnit.assertEquals(20, me.getLife());
    }

    @Test
    public void testThreeLifeTriggersTheHost() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        combined(game, me);
        opponentLoses(game, me, game.getPlayers().get(0), 3);
        finishTurn(game, me);
        AssertJUnit.assertTrue("life " + me.getLife(), me.getLife() >= 21 && me.getLife() <= 26);
    }
}
