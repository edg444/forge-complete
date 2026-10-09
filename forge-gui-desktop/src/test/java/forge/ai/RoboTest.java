package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Robo- (Unstable): Oracle text and the augment rulings checked against Scryfall 2026-10-09. Adorable Kitten is the
 * host: "When this creature enters, roll a six-sided die. You gain life equal to the result." Augmenting doesn't
 * count as an artifact entering (user ruling 2026-10-09).
 */
public class RoboTest extends AITest {

    private Card combined(Game game, Player me) {
        Card kitten = addCard("Adorable Kitten", me);
        Card robo = addCardToZone("Robo-", me, ZoneType.Hand);
        for (SpellAbility sa : robo.getSpellAbilities()) {
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

    @Test
    public void testCombinedCreature() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card c = combined(game, me);

        AssertJUnit.assertEquals("Robo-Kitten", c.getName());
        AssertJUnit.assertEquals(2, c.getNetPower());
        AssertJUnit.assertEquals(2, c.getNetToughness());
        AssertJUnit.assertTrue(c.isArtifact());
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Construct"));
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Cat"));
        AssertJUnit.assertTrue(c.getOracleText().contains("At the beginning of each end step, if an artifact entered "
                + "the battlefield under your control this turn, roll a six-sided die. You gain life equal to the result."));
    }

    @Test
    public void testAugmentingItselfDoesntCount() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);
        combined(game, me);

        finishTurn(game, me);
        AssertJUnit.assertEquals(20, me.getLife());
    }

    @Test
    public void testHostEffectAfterAnArtifactEnters() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);
        combined(game, me);

        // an opponent's artifact doesn't count
        game.getAction().moveToPlay(addCardToZone("Ornithopter", opp, ZoneType.Hand), null, null);
        finishTurn(game, me);
        AssertJUnit.assertEquals(20, me.getLife());

        // "each end step": the opponent's turn, once one of mine enters
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        int guard = 0;
        game.getAction().moveToPlay(addCardToZone("Ornithopter", me, ZoneType.Hand), null, null);
        while (!game.isGameOver() && game.getPhaseHandler().getPlayerTurn() == opp) {
            game.getPhaseHandler().mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 500);
        }
        AssertJUnit.assertTrue("life " + me.getLife(), me.getLife() >= 21 && me.getLife() <= 26);
    }
}
