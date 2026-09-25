package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Ruling: when gained text boxes contradict, the most recently added one wins. Beast of Burden sets P/T
 * to the number of creatures on the battlefield (3 here), Psychosis Crawler to cards in hand (5 here).
 */
public class DoItYourselfSeraphTest extends AITest {

    private int seraphPowerAfterExiling(String first, boolean firstFromOpponent, String second, boolean secondFromOpponent) {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card seraph = addCard("Do-It-Yourself Seraph", me);
        addCard("Grizzly Bears", me);
        addCard("Grizzly Bears", opp);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Plains", me, ZoneType.Hand);
        }
        Trigger attack = null;
        for (Trigger t : seraph.getTriggers()) {
            attack = t;
        }
        SpellAbility search = attack.ensureAbility();
        String[] names = {first, second};
        boolean[] fromOpponent = {firstFromOpponent, secondFromOpponent};
        for (int i = 0; i < 2; i++) {
            // "your library" is the searching player's, so a stolen Seraph fills a different exile zone
            Player searcher = fromOpponent[i] ? opp : me;
            addCardToZone(names[i], searcher, ZoneType.Library);
            search.setActivatingPlayer(searcher);
            AbilityUtils.resolve(search);
            game.getAction().checkStateEffects(true);
        }
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(seraph.getNetPower(), seraph.getNetToughness());
        return seraph.getNetPower();
    }

    @Test
    public void testMostRecentlyGainedTextBoxWins() {
        AssertJUnit.assertEquals(5, seraphPowerAfterExiling("Beast of Burden", false, "Psychosis Crawler", false));
        AssertJUnit.assertEquals(3, seraphPowerAfterExiling("Psychosis Crawler", false, "Beast of Burden", false));
    }

    @Test
    public void testOrderHoldsAcrossExileZones() {
        AssertJUnit.assertEquals(5, seraphPowerAfterExiling("Beast of Burden", false, "Psychosis Crawler", true));
        AssertJUnit.assertEquals(3, seraphPowerAfterExiling("Psychosis Crawler", false, "Beast of Burden", true));
    }
}
