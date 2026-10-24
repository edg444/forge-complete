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

/** AI:RemoveDeck:All cards, eighth batch (B...): each test is the situation that card's new AI is for. */
public class NoAiBatch08Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private static SpellAbility activated(Card c) {
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

    // Bond of Agony: "As an additional cost to cast this spell, pay X life. Each other player loses X life."
    @Test
    public void testBondOfAgonyOnlyForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 8);
        SpellAbility sa = addCardToZone("Bond of Agony", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(6, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(6), sa.getRootAbility().getXManaCostPaid());
        ai.setLife(6, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Bloodflow Connoisseur: "Sacrifice a creature: Put a +1/+1 counter on this creature."
    @Test
    public void testBloodflowConnoisseurEatsACreatureThatIsDying() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card connoisseur = addCard("Bloodflow Connoisseur", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = activated(connoisseur);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.addBlocker(wurm, bears);
        combat.setBlocked(wurm, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
