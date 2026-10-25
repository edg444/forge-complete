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

/** Second pass over the remaining todos, first batch. */
public class NoAiBatch84Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Black Sun's Zenith: X -1/-1 counters on each creature - enough for theirs, sparing its Craw Wurm
    @Test
    public void testBlackSunsZenithPicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 8; i++) {
            addCard("Swamp", ai);
        }
        addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Black Sun's Zenith", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(3, sa.getXManaCostPaid().intValue());
    }

    // Abandon Hope: X of their cards for X of its spare lands, X paid in mana too
    @Test
    public void testAbandonHopeTradesSpareLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 7; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Swamp", ai, ZoneType.Hand);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        }
        SpellAbility sa = addCardToZone("Abandon Hope", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Azorius Ploy: no damage from their blocker, and none to its attacker
    @Test
    public void testAzoriusPloySavesTheAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Plains", ai);
            addCard("Island", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Azorius Ploy", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
