package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 111: AILogic$ GrantAbility (GrantAbilityAi), spells that hand creatures an ability or trigger. */
public class NoAiBatch111Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static void fight(Game game, Player attacker, Card att, Player defender, Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        if (blocker != null) {
            combat.addBlocker(att, blocker);
        }
        combat.setBlocked(att, blocker != null);
        game.getPhaseHandler().setCombat(combat);
    }

    // Resuscitate: its Serra Angel blocking their Craw Wurm would die, and a Forest is left to regenerate it
    @Test
    public void testResuscitateSavesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Resuscitate", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        fight(game, opp, wurm, ai, angel);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Showstopper: its Bears die to the blocking Hill Giant, and the 2 damage kills their Llanowar Elves
    @Test
    public void testShowstopperWhenItsCreaturesDie() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Badlands", ai);
        addCard("Badlands", ai);
        addCard("Badlands", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        addCard("Llanowar Elves", opp);
        SpellAbility sa = addCardToZone("Showstopper", ai, ZoneType.Hand).getFirstSpellAbility();
        fight(game, ai, bears, opp, giant);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Retraction Helix: its idle Bears get the tap-to-bounce, with their Serra Angel worth bouncing
    @Test
    public void testRetractionHelixOnAnIdleCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        SpellAbility sa = addCardToZone("Retraction Helix", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame("nothing of theirs worth it", AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Serra Angel", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Shade's Breath: its unblocked Bears with black mana to spare
    @Test
    public void testShadesBreathInCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Shade's Breath", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        fight(game, ai, bears, opp, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
