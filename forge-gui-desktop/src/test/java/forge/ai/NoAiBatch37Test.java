package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, batches 37-41 (J-M): each test is the situation that card's new AI is for. */
public class NoAiBatch37Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Kaervek's Spite: "As an additional cost to cast this spell, sacrifice all permanents you control and discard your hand.
    // Target player loses 5 life."
    @Test
    public void testKaerveksSpiteOnlyForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 3);
        addCard("Hill Giant", ai);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Kaervek's Spite", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(5, null);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Kaboom!: "Choose any number of target players or planeswalkers. For each of them, reveal cards from the top of your
    // library until you reveal a nonland card, ..."
    @Test
    public void testKaboomTargetsOnlyOpponents() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 5);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Hill Giant", ai, ZoneType.Library);
        }
        SpellAbility sa = addCardToZone("Kaboom!", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(1, sa.getTargets().size());
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Kiku's Shadow: "Target creature deals damage to itself equal to its power."
    @Test
    public void testKikuTargetsACreatureItsOwnPowerKills() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 2);
        addCard("Wall of Stone", opp);
        SpellAbility sa = addCardToZone("Kiku's Shadow", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        Card giant = addCard("Hill Giant", opp);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
    }

    // Knollspine Dragon: "When this creature enters, you may discard your hand and draw cards equal to the damage dealt
    // to target opponent this turn."
    @Test
    public void testKnollspineOnlyTradesTheHandForMore() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        Card dragon = addCard("Knollspine Dragon", ai);
        SpellAbility sa = null;
        for (Trigger t : dragon.getTriggers()) {
            sa = t.ensureAbility();
        }
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, false));
        CardDamageTable damage = new CardDamageTable();
        damage.put(addCard("Hill Giant", ai), opp, 4);
        game.getAction().dealDamage(true, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, false));
    }

    // Manamorphose: "Add two mana in any combination of colors. Draw a card."
    @Test
    public void testManamorphoseCyclesIntoAnotherSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Mountain", 4);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        SpellAbility sa = addCardToZone("Manamorphose", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Magmasaur: "enters with five +1/+1 counters ... At the beginning of your upkeep, you may remove a +1/+1 counter
    // from it. If you don't, sacrifice it ..." - the upkeep cost is paid with the counters it comes with
    @Test
    public void testMagmasaurIsCastDespiteItsCounterUpkeep() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Mountain", 5);
        SpellAbility sa = addCardToZone("Magmasaur", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
