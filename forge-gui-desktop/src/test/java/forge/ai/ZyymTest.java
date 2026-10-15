package forge.ai;

import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;

import forge.ai.ability.RevealUntilStopAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Zyym, Mesmeric Lord (CMB1 #104), checked against Scryfall and its ruling 2026-10-15: the opponent orders their
 * hand and reveals it one by one until you say stop, then discards the last card revealed.
 */
public class ZyymTest extends AITest {

    private static SpellAbility etb(Card zyym, Player you, Player target) {
        Trigger t = zyym.getTriggers().iterator().next();
        SpellAbility sa = t.ensureAbility();
        sa.setActivatingPlayer(you);
        sa.getTargets().add(target);
        return sa;
    }

    @Test
    public void discardsExactlyOneRevealedCard() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card zyym = addCard("Zyym, Mesmeric Lord", ai);
        CardCollection hand = new CardCollection();
        for (String name : new String[] {"Forest", "Grizzly Bears", "Shivan Dragon", "Island", "Shock"}) {
            hand.add(addCardToZone(name, opp, ZoneType.Hand));
        }
        AbilityUtils.resolve(etb(zyym, ai, opp));
        AssertJUnit.assertEquals(4, opp.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertTrue(hand.stream().anyMatch(c -> game.getCardState(c).isInZone(ZoneType.Graveyard)));
    }

    @Test
    public void aLoneCardIsDiscardedAndAnEmptyHandIsFine() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card zyym = addCard("Zyym, Mesmeric Lord", ai);
        AbilityUtils.resolve(etb(zyym, ai, opp));
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Graveyard).isEmpty());

        Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        AbilityUtils.resolve(etb(zyym, ai, opp));
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void aiStopsLikeTheSecretaryProblem() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card forest = addCardToZone("Forest", opp, ZoneType.Hand);
        Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        Card dragon = addCardToZone("Shivan Dragon", opp, ZoneType.Hand);
        Card zyym = addCard("Zyym, Mesmeric Lord", ai);
        SpellAbility sa = etb(zyym, ai, opp);
        RevealUntilStopAi logic = new RevealUntilStopAi();

        AssertJUnit.assertFalse("the first of five goes by", logic.confirmAction(ai, sa, null, "", params(5, dragon)));
        AssertJUnit.assertFalse("never a land", logic.confirmAction(ai, sa, null, "", params(5, bears, forest)));
        AssertJUnit.assertTrue("better than all so far", logic.confirmAction(ai, sa, null, "", params(5, bears, dragon)));
        AssertJUnit.assertFalse("worse than one let go", logic.confirmAction(ai, sa, null, "", params(5, dragon, bears)));
        AssertJUnit.assertTrue("of two, take a spell at once", logic.confirmAction(ai, sa, null, "", params(2, bears)));
    }

    private static Map<String, Object> params(int handSize, Card... revealed) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("Revealed", new CardCollection(java.util.Arrays.asList(revealed)));
        params.put("HandSize", handSize);
        return params;
    }
}
