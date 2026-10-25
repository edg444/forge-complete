package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 132: EndgameAi and the last Sokenzan sweeps. */
public class NoAiBatch132Test extends AITest {

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

    private static Combat combat(Game game, Player attacker, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, attacker, false, 1);
        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Burn at the Stake: four idle creatures are 12 damage, lethal at 10
    @Test
    public void testBurnAtTheStakeForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Grizzly Bears", ai);
        }
        opp.setLife(10, null);
        SpellAbility sa = addCardToZone("Burn at the Stake", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Dead Ringers: their two green creatures
    @Test
    public void testDeadRingersOnASameColorPair() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Craw Wurm", opp);
        addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Dead Ringers", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().allMatch(c -> c.getColor().hasGreen()));
    }

    // Disharmony: their attacking Serra Angel becomes its own
    @Test
    public void testDisharmonyStealsTheAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Disharmony", ai, ZoneType.Hand).getFirstSpellAbility();
        Combat c = combat(game, opp, PhaseType.COMBAT_DECLARE_ATTACKERS);
        c.addAttacker(angel, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Akul the Unrepentant: three spare Llanowar Elves for the Craw Wurm in hand
    @Test
    public void testAkulCheatsInTheBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card akul = addCard("Akul the Unrepentant", ai);
        for (int i = 0; i < 3; i++) {
            addCard("Llanowar Elves", ai).setSVar("SacMe", "1");
        }
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        SpellAbility sa = ability(akul);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(3, SacrificePayoffAi.planned(sa).size());
    }

    // Cataclysm: they keep one of each, losing three Hill Giants; it has one of each already
    @Test
    public void testCataclysmWhenTheyLoseMore() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        addCard("Serra Angel", ai);
        addCard("Mountain", opp);
        for (int i = 0; i < 4; i++) {
            addCard("Hill Giant", opp);
        }
        SpellAbility sa = addCardToZone("Cataclysm", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Plow Through Reito: its unblocked Bears and 3 life to go need one Plains... at +1/+1 a Plains, so one
    @Test
    public void testPlowThroughReitoForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Plains", ai);
        }
        opp.setLife(3, null);
        Card bears = addCard("Grizzly Bears", ai);
        Combat c = combat(game, ai, PhaseType.COMBAT_DECLARE_BLOCKERS);
        c.addAttacker(bears, opp);
        c.setBlocked(bears, false);
        SpellAbility sa = addCardToZone("Plow Through Reito", ai, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getSubAbility().getTargetCard());
    }

    // Yare: its Grizzly Bears at +3 kill their attacking Hill Giant
    @Test
    public void testYareOnABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        Combat c = combat(game, opp, PhaseType.COMBAT_DECLARE_ATTACKERS);
        c.addAttacker(giant, ai);
        SpellAbility sa = addCardToZone("Yare", ai, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }
}
