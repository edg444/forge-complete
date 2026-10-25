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

/** Second pass, batch 118: AILogic$ PreventX (Festival of the Guildpact, Guardian Angel). */
public class NoAiBatch118Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Guardian Angel: its Serra Angel blocking a Craw Wurm takes 6, so X of 3 saves it
    @Test
    public void testGuardianAngelSavesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Guardian Angel", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.addBlocker(wurm, angel);
        combat.setBlocked(wurm, true);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(3), sa.getXManaCostPaid());
    }

    // Festival of the Guildpact: at 3 life against an unblocked Wurm, X of 4 keeps it alive
    @Test
    public void testFestivalPreventsLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        for (int i = 0; i < 6; i++) {
            addCard("Plains", ai);
        }
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Festival of the Guildpact", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(4), sa.getXManaCostPaid());
    }

    // Festival of the Guildpact: nothing to prevent, so X of 0 at their end step for the card
    @Test
    public void testFestivalAsACantrip() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        SpellAbility sa = addCardToZone("Festival of the Guildpact", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(0), sa.getXManaCostPaid());
    }
}
