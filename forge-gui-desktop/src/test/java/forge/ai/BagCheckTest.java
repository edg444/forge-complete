package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Bag Check (UNF #36), checked against Scryfall 2026-10-21 (no rulings): counter target spell, then a person outside
 * the game says whether it looks dangerous - a loot if so. The AI's simulated person says yes half the time.
 */
public class BagCheckTest extends AITest {

    @Test
    public void testCountersAndMaybeLoots() {
        int looted = 0;
        for (int i = 0; i < 30; i++) {
            final Game game = initAndCreateGame();
            final Player ai = game.getPlayers().get(1);
            final Player opp = game.getPlayers().get(0);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
            for (int j = 0; j < 3; j++) {
                addCardToZone("Island", ai, ZoneType.Library);
            }
            addCardToZone("Island", ai, ZoneType.Hand);

            Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
            final SpellAbility boltSa = bolt.getFirstSpellAbility();
            boltSa.setActivatingPlayer(opp);
            boltSa.getTargets().add(ai);
            bolt = game.getAction().moveToStack(bolt, boltSa);
            boltSa.setHostCard(bolt);
            game.getStack().add(boltSa);

            final Card check = addCardToZone("Bag Check", ai, ZoneType.Hand);
            final SpellAbility sa = check.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            sa.getTargets().add(game.getStack().peekAbility());
            final int life = ai.getLife();
            AbilityUtils.resolve(sa);
            AssertJUnit.assertTrue("countered", game.getCardState(bolt).isInZone(ZoneType.Graveyard));
            AssertJUnit.assertEquals(life, ai.getLife());
            if (ai.getCardsIn(ZoneType.Library).size() == 2) {
                looted++;
                AssertJUnit.assertEquals("drew, then discarded", 1, ai.getCardsIn(ZoneType.Graveyard).size());
            }
        }
        AssertJUnit.assertTrue("the simulated person says yes some of the time, not always: " + looted,
                looted > 0 && looted < 30);
    }
}
