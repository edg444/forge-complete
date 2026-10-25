package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.MiscSpellAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 131: FinalCardAi, Demoralize, Fossil Find. */
public class NoAiBatch131Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Fire Covenant: 4 life kills their two Bears, keeping it well above 8
    @Test
    public void testFireCovenantKillsForLife() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card a = addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Fire Covenant", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
        AssertJUnit.assertEquals(Integer.valueOf(2), sa.getDividedValue(a));
    }

    // Prismatic Strands: their green attackers' 9 damage, so green
    @Test
    public void testPrismaticStrandsNamesTheirColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = addCardToZone("Prismatic Strands", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        for (String n : new String[] {"Craw Wurm", "Hill Giant"}) {
            Card c = addCard(n, opp);
            combat.addAttacker(c, ai);
            combat.setBlocked(c, false);
        }
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("green", MiscSpellAi.plannedColor(sa));
    }

    // Hallow: their Lightning Bolt on the stack
    @Test
    public void testHallowOnTheirBurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = addCardToZone("Hallow", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(opp);
        bolt.getTargets().add(ai);
        game.getStack().add(bolt);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Demoralize: without threshold, menace leaves half their blockers; with seven in its graveyard none can block
    @Test
    public void testDemoralizeAtThreshold() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Grizzly Bears", ai).setSickness(false);
        }
        addCard("Hill Giant", opp);
        addCard("Hill Giant", opp);
        opp.setLife(6, null);
        SpellAbility sa = addCardToZone("Demoralize", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        for (int i = 0; i < 7; i++) {
            addCardToZone("Swamp", ai, ZoneType.Graveyard);
        }
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Fossil Find: no longer crashes the AI, and a card back from its graveyard is worth the spell
    @Test
    public void testFossilFindDecides() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Fossil Find", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotNull(decide(ai, sa));
    }

    // Wake to Slaughter: its graveyard's Serra Angel and Craw Wurm
    @Test
    public void testWakeToSlaughter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Wake to Slaughter", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }
}
