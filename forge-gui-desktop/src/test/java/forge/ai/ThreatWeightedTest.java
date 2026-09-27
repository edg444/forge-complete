package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardThreat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Defective Detective's person outside the game: a random card from the hand, likelier the more impactful it is.
 */
public class ThreatWeightedTest extends AITest {

    @Test
    public void testImpactScaleAcrossCardTypes() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Card forest = addCardToZone("Forest", opp, ZoneType.Hand);
        Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        Card shivan = addCardToZone("Shivan Dragon", opp, ZoneType.Hand);
        Card wrath = addCardToZone("Wrath of God", opp, ZoneType.Hand);

        // the GUI layer plugs the AI's evaluation in, so creatures and other spells share a scale
        AssertJUnit.assertEquals(ComputerUtilCard.evaluateCardImpact(shivan), CardThreat.evaluate(shivan));
        AssertJUnit.assertTrue(CardThreat.evaluate(forest) < CardThreat.evaluate(bears));
        AssertJUnit.assertTrue(CardThreat.evaluate(bears) < CardThreat.evaluate(wrath));
        AssertJUnit.assertTrue(CardThreat.evaluate(wrath) < CardThreat.evaluate(shivan));
    }

    @Test
    public void testDetectivePicksFavorImpact() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card forest = addCardToZone("Forest", opp, ZoneType.Hand);
        Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        Card shivan = addCardToZone("Shivan Dragon", opp, ZoneType.Hand);
        Card detective = addCard("Defective Detective", me);

        SpellAbility sa = null;
        for (Trigger t : detective.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone) {
                sa = t.ensureAbility();
            }
        }
        sa.setActivatingPlayer(me);
        sa.getTargets().add(opp);

        int nForest = 0, nBears = 0, nShivan = 0;
        for (int i = 0; i < 3000; i++) {
            AbilityUtils.resolve(sa);
            AssertJUnit.assertEquals(1, detective.getChosenCards().size());
            Card chosen = detective.getChosenCards().get(0);
            if (chosen.equals(forest)) {
                nForest++;
            } else if (chosen.equals(bears)) {
                nBears++;
            } else if (chosen.equals(shivan)) {
                nShivan++;
            }
        }
        AssertJUnit.assertEquals(3000, nForest + nBears + nShivan);
        // random, not always the best: every card comes up, but in order of impact
        AssertJUnit.assertTrue(nForest > 0);
        AssertJUnit.assertTrue(nForest < nBears);
        AssertJUnit.assertTrue(nBears < nShivan);
    }
}
