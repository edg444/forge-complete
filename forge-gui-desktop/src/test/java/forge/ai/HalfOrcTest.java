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
 * Half-Orc, Half- (Unstable): "Trample / At the beginning of each end step, if an opponent was dealt damage this turn,
 * / Augment {1}{R}{R}" - Oracle text and the augment rulings checked against Scryfall 2026-10-02. Adorable Kitten is the
 * host: "When this creature enters, roll a six-sided die. You gain life equal to the result."
 */
public class HalfOrcTest extends AITest {

    private Card combined(Game game, Player me) {
        Card kitten = addCard("Adorable Kitten", me);
        Card orc = addCardToZone("Half-Orc, Half-", me, ZoneType.Hand);
        for (SpellAbility sa : orc.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                sa.setActivatingPlayer(me);
                sa.getTargets().add(kitten);
                AbilityUtils.resolve(sa);
            }
        }
        game.getAction().checkStateEffects(true);
        return kitten;
    }

    /** Plays out the rest of the turn from the second main phase. */
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

        AssertJUnit.assertEquals("Half-Orc, Half-Kitten", c.getName());
        // 1/1 host + 3/1
        AssertJUnit.assertEquals(4, c.getNetPower());
        AssertJUnit.assertEquals(2, c.getNetToughness());
        AssertJUnit.assertTrue(c.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Orc"));
        AssertJUnit.assertTrue(c.getType().hasCreatureType("Cat"));
        AssertJUnit.assertTrue(c.getColor().hasRed() && c.getColor().hasWhite());
        AssertJUnit.assertTrue(c.getOracleText().contains("At the beginning of each end step, if an opponent was dealt "
                + "damage this turn, roll a six-sided die. You gain life equal to the result."));
    }

    @Test
    public void testNothingWithoutDamageToAnOpponent() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        combined(game, me);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);

        finishTurn(game, me);
        AssertJUnit.assertEquals(20, me.getLife());
    }

    @Test
    public void testHostEffectAtEndStepAfterDamage() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        combined(game, me);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);
        SpellAbility shock = addCardToZone("Shock", me, ZoneType.Hand).getFirstSpellAbility();
        shock.setActivatingPlayer(me);
        shock.getTargets().add(opp);
        AbilityUtils.resolve(shock);
        AssertJUnit.assertEquals(18, opp.getLife());

        finishTurn(game, me);
        AssertJUnit.assertTrue("life " + me.getLife(), me.getLife() >= 21 && me.getLife() <= 26);
    }
}
