package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 124: XAbilityAi and the sacrifice/reveal payoffs. */
public class NoAiBatch124Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c, String api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility() && (api == null || sa.getApi().toString().equals(api))) {
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

    // Painbringer: three graveyard cards exiled kill their Hill Giant
    @Test
    public void testPainbringerExilesJustEnough() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", opp);
        Card pain = addCard("Painbringer", ai);
        pain.setSickness(false);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Swamp", ai, ZoneType.Graveyard);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        SpellAbility sa = ability(pain, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(3), sa.getXManaCostPaid());
    }

    // Simic Manipulator: two counters take their 2-power Bears, not the 3-power Giant
    @Test
    public void testSimicManipulatorStealsWithinItsCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        Card sim = addCard("Simic Manipulator", ai);
        sim.setSickness(false);
        sim.addCounterInternal(CounterEnumType.P1P1, 2, ai, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        SpellAbility sa = ability(sim, "GainControl");
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Firestorm: two spare lands discarded for 2 damage each to their two Bears
    @Test
    public void testFirestormKillsForSpareCards() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 7);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Firestorm", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Mountain", ai, ZoneType.Hand);
        addCardToZone("Mountain", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }

    // Exponential Growth: their 9 life and its unblocked 2-power Bears need three doublings (16), {X}{X} of 3 from eight lands
    @Test
    public void testExponentialGrowthForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 8);
        opp.setLife(9, null);
        Card bears = addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Exponential Growth", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(3), sa.getXManaCostPaid());
    }

    // Titan's Presence: revealing its 4-power colorless creature exiles their 4-power Serra Angel
    @Test
    public void testTitansPresenceRevealsTheBiggest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Titan's Presence", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Ornithopter", ai, ZoneType.Hand);
        Card golem = addCardToZone("Juggernaut", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(golem, SacrificePayoffAi.planned(sa).get(0));
    }

    // Forge Armor: its spare 4-drop artifact's counters go on its Serra Angel
    @Test
    public void testForgeArmorTargetsItsBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Mountain", 5);
        Card angel = addCard("Serra Angel", ai);
        Card stone = addCard("Thran Dynamo", ai);
        stone.setSVar("SacMe", "1");
        SpellAbility sa = addCardToZone("Forge Armor", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Foul-Tongue Shriek: three attackers drain 3
    @Test
    public void testFoulTongueShriekInCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = addCardToZone("Foul-Tongue Shriek", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        for (int i = 0; i < 3; i++) {
            Card b = addCard("Grizzly Bears", ai);
            combat.addAttacker(b, opp);
            combat.setBlocked(b, false);
        }
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Droning Bureaucrats: on their turn, X of 4 keeps their Hill Giant from attacking
    @Test
    public void testDroningBureaucratsStopsTheirAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        addCard("Hill Giant", opp);
        Card dron = addCard("Droning Bureaucrats", ai);
        dron.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, opp);
        SpellAbility sa = ability(dron, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }
}
