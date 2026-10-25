package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.NumberGuessAi;
import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 125: XAbilityAi group C, Liquid Fire, Impelled Giant, RescueAi, Minamo Sightbender. */
public class NoAiBatch125Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    private static Combat combat(Game game, Player attacker, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, attacker, false, 1);
        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Liquid Fire: 3 kills their Hill Giant, the other 2 to their face
    @Test
    public void testLiquidFireSplits() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Liquid Fire", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(3), NumberGuessAi.plannedNumber(sa));
    }

    // Entrancing Lyre: X of 4 locks their Serra Angel
    @Test
    public void testEntrancingLyreLocksTheirBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 5);
        Card angel = addCard("Serra Angel", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = ability(addCard("Entrancing Lyre", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Biomass Mutation: two unblocked Elves need base 4/4 against 8 life
    @Test
    public void testBiomassMutationForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 8);
        opp.setLife(8, null);
        Combat c = combat(game, ai, PhaseType.COMBAT_DECLARE_BLOCKERS);
        for (int i = 0; i < 2; i++) {
            Card elf = addCard("Llanowar Elves", ai);
            c.addAttacker(elf, opp);
            c.setBlocked(elf, false);
        }
        SpellAbility sa = addCardToZone("Biomass Mutation", ai, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Gift of Tusks: their Serra Angel is worth more than a vanilla 3/3; their Bears aren't
    @Test
    public void testGiftOfTusksOnTheirBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Gift of Tusks", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card angel = addCard("Serra Angel", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Impelled Giant: blocked by a 4-toughness Serra Angel, it taps its idle Hill Giant (3 power) to kill it
    @Test
    public void testImpelledGiantTapsForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Impelled Giant", ai);
        Card helper = addCard("Hill Giant", ai);
        Card angel = addCard("Serra Angel", opp);
        Combat c = combat(game, ai, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(giant, opp);
        c.addBlocker(giant, angel);
        c.setBlocked(giant, true);
        SpellAbility sa = ability(giant);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(helper, SacrificePayoffAi.planned(sa).get(0));
    }

    // Planar Guide: exiles everything when their attack would kill it
    @Test
    public void testPlanarGuideDodgesLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        ai.setLife(4, null);
        SpellAbility sa = ability(addCard("Planar Guide", ai));
        Card wurm = addCard("Craw Wurm", opp);
        Combat c = combat(game, opp, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(wurm, ai);
        c.setBlocked(wurm, false);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Scapegoat: in answer to their Wrath of God, its Bears go so the Angel and Giant come back to hand
    @Test
    public void testScapegoatSavesFromAWrath() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", ai);
        addCard("Hill Giant", ai);
        SpellAbility sa = addCardToZone("Scapegoat", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility wrath = addCardToZone("Wrath of God", opp, ZoneType.Hand).getFirstSpellAbility();
        wrath.setActivatingPlayer(opp);
        game.getStack().add(wrath);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(angel));
        AssertJUnit.assertEquals(bears, SacrificePayoffAi.planned(sa).get(0));
    }

    // Outmaneuver: its Craw Wurm blocked by a Bears deals its 6 to their 6 life
    @Test
    public void testOutmaneuverForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 3);
        opp.setLife(6, null);
        Card wurm = addCard("Craw Wurm", ai);
        Card bears = addCard("Grizzly Bears", opp);
        Combat c = combat(game, ai, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(wurm, opp);
        c.addBlocker(wurm, bears);
        c.setBlocked(wurm, true);
        SpellAbility sa = addCardToZone("Outmaneuver", ai, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(1), sa.getXManaCostPaid());
    }

    // Burden of Guilt: the curse goes on their creature
    @Test
    public void testBurdenOfGuiltOnTheirCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Burden of Guilt", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Minamo Sightbender: X of 2 makes its Bears unblockable for the last 2 life
    @Test
    public void testMinamoSightbenderSetsX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 4);
        opp.setLife(2, null);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        addCard("Hill Giant", opp);
        Card minamo = addCard("Minamo Sightbender", ai);
        minamo.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        SpellAbility sa = ability(minamo);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(2), sa.getXManaCostPaid());
    }
}
