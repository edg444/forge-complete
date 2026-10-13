package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Everlasting Lich (CMB1 #41), a 4/0 that "can't die": indestructible, can't be sacrificed, and exempt from the
 * 0-toughness state-based action (only that one - Rule$ ZeroToughness), and can't block.
 */
public class EverlastingLichTest extends AITest {

    @Test
    public void survivesZeroToughnessButOtherRulesStillApply() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card lich = addCard("Everlasting Lich", ai);
        Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(lich.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, lich.getNetToughness());
        AssertJUnit.assertTrue(lich.hasKeyword(Keyword.INDESTRUCTIBLE));
        AssertJUnit.assertFalse(lich.canBeSacrificedBy(null, true));

        // the narrow exemption isn't a blanket one: +1/+1 and -1/-1 counters still cancel out
        lich.addCounterInternal(forge.game.card.CounterEnumType.P1P1, 1, ai, false, null, null);
        lich.addCounterInternal(forge.game.card.CounterEnumType.M1M1, 1, ai, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(0, lich.getCounters(forge.game.card.CounterEnumType.P1P1));

        // and other creatures still die at 0 toughness
        bears.addCounterInternal(forge.game.card.CounterEnumType.M1M1, 2, ai, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(lich).isInZone(ZoneType.Battlefield));

        String text = lich.getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Everlasting Lich can't die. (This creature has indestructible"));
    }

    @Test
    public void aiCastsIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Swamp", 5, ai);
        addCardToZone("Everlasting Lich", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.END_OF_TURN);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Everlasting Lich"));
    }
}
