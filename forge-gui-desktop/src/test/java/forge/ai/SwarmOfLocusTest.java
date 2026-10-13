package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;

/**
 * Swarm of Locus (CMB1 #48), checked against Scryfall and its ruling 2026-10-13: Locus is a creature type here as well
 * as a land type, and the Swarm counts both, while other cards that refer to one don't see the other (Cloudpost counts
 * Locus lands only).
 */
public class SwarmOfLocusTest extends AITest {

    @Test
    public void countsLocusCreaturesAndLandsButCloudpostCountsLandsOnly() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card swarm = addCard("Swarm of Locus", ai);
        addCard("Swarm of Locus", ai);
        Card cloudpost = addCard("Cloudpost", ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("the creature type survives", swarm.getType().hasCreatureType("Locus"));
        AssertJUnit.assertTrue(cloudpost.getType().hasSubtype("Locus"));

        int swarmCount = AbilityUtils.calculateAmount(swarm, "X", swarm.getTriggers().get(0).ensureAbility());
        AssertJUnit.assertEquals("two Swarms and a Cloudpost", 3, swarmCount);

        int cloudpostCount = AbilityUtils.calculateAmount(cloudpost, "X", cloudpost.getFirstSpellAbility());
        AssertJUnit.assertEquals("Cloudpost sees only the Locus land", 1, cloudpostCount);
    }
}
