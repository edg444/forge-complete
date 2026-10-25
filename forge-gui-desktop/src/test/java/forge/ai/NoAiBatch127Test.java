package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.DiscardAnyAi;
import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 127: HandAi, Nahiri's Wrath, Nantuko Cultivator, Caldera Hellion. */
public class NoAiBatch127Test extends AITest {

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

    // Venomous Breath: on its Bears, whose blocker is their Serra Angel
    @Test
    public void testVenomousBreathKillsTheBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Venomous Breath", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, angel);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Descent of the Dragons: its two Llanowar Elves become 4/4 flying Dragons
    @Test
    public void testDescentOfTheDragonsUpgradesItsOwn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card a = addCard("Llanowar Elves", ai);
        addCard("Llanowar Elves", ai);
        SpellAbility sa = addCardToZone("Descent of the Dragons", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(a));
    }

    // Meteor Shower: X of 2 is 3 damage, their Bears killed and 1 to their face
    @Test
    public void testMeteorShowerDivides() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 5);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Meteor Shower", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
        AssertJUnit.assertEquals(Integer.valueOf(2), sa.getDividedValue(bears));
    }

    // Ruthless Disposal: their Serra Angel and Craw Wurm for its Llanowar Elves
    @Test
    public void testRuthlessDisposalTradesUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 5);
        Card elves = addCard("Llanowar Elves", ai);
        addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Ruthless Disposal", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
        AssertJUnit.assertEquals(elves, SacrificePayoffAi.planned(sa).get(0));
    }

    // Nahiri's Wrath: discarding its Craw Wurm (6) kills their Serra Angel
    @Test
    public void testNahirisWrathPlansTheDiscard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 4);
        Card angel = addCard("Serra Angel", opp);
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Nahiri's Wrath", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));
    }

    // Aladdin's Lamp: only in its upkeep, before the draw
    @Test
    public void testAladdinsLampInUpkeep() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Island", 4);
        SpellAbility sa = ability(addCard("Aladdin's Lamp", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Caldera Hellion: not into its own board of small creatures
    @Test
    public void testCalderaHellionOnlyWhenTheSweepHelps() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 5);
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Caldera Hellion", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        for (int i = 0; i < 3; i++) {
            addCard("Hill Giant", opp);
        }
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Nantuko Cultivator: with seven lands out, both lands in hand go for counters and cards
    @Test
    public void testNantukoCultivatorDiscardsSpareLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Forest", 7);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCardToZone("Forest", ai, ZoneType.Hand);
        Card nantuko = addCardToZone("Nantuko Cultivator", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        SpellAbility trig = forge.game.ability.AbilityFactory.getAbility(nantuko.getSVar("TrigNantukoDiscardLand"), nantuko);
        trig.setActivatingPlayer(ai);
        DiscardAnyAi.decide(ai, trig);
        AssertJUnit.assertEquals(Integer.valueOf(2), DiscardAnyAi.planned(trig));
    }
}
