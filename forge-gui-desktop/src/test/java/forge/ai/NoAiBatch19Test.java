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

/** AI:RemoveDeck:All cards, batches 14-19 (C-D): each test is the situation that card's new AI is for. */
public class NoAiBatch19Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Brutal Deceiver: "{1}: Look at the top card of your library." / "{2}: Reveal the top card of your library. If it's a
    // land card, this creature gets +1/+0 and gains first strike until end of turn. Activate only once each turn."
    @Test
    public void testDeceiverPeeksOnceAndRevealsOnlyInCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 4);
        fillLibrary(ai, 5);
        Card deceiver = addCard("Brutal Deceiver", ai);
        deceiver.setSickness(false);
        SpellAbility peek = null;
        SpellAbility reveal = null;
        for (SpellAbility sa : deceiver.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                if (sa.hasParam("RememberRevealed")) {
                    reveal = sa;
                } else {
                    peek = sa;
                }
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, peek));
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, reveal));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, peek));

        Card bears = addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(deceiver, opp);
        combat.addBlocker(deceiver, bears);
        combat.setBlocked(deceiver, true);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, reveal));
    }

    // Dangerous Wager: "Discard your hand, then draw two cards."
    @Test
    public void testDangerousWagerWithAtMostOneOtherCard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Mountain", 2);
        fillLibrary(ai, 10);
        SpellAbility sa = addCardToZone("Dangerous Wager", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals("Grizzly Bears")) {
                game.getAction().moveToGraveyard(c, null);
                break;
            }
        }
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
