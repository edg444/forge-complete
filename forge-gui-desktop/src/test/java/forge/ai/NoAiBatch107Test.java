package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.DiscardAnyAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 107: AILogic$ DiscardAny (DiscardAnyAi), the Odyssey Rites. */
public class NoAiBatch107Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Sacred Rites: its Bears blocking their Hill Giant live with two cards' +0/+2, lands it has no use for
    @Test
    public void testSacredRitesSavesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 7; i++) {
            addCard("Plains", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Sacred Rites", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Plains", ai, ZoneType.Hand);
        addCardToZone("Plains", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(giant, ai);
        combat.addBlocker(giant, bears);
        combat.setBlocked(giant, true);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(2), DiscardAnyAi.planned(sa));
    }

    // Rites of Initiation: two unblocked Bears and 6 life to go, so one discard (+1/+0 each) is lethal
    @Test
    public void testRitesOfInitiationForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.setLife(6, null);
        Card a = addCard("Grizzly Bears", ai);
        Card b = addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Rites of Initiation", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        addCardToZone("Mountain", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(a, opp);
        combat.addAttacker(b, opp);
        combat.setBlocked(a, false);
        combat.setBlocked(b, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(1), DiscardAnyAi.planned(sa));

        opp.setLife(20, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Rites of Spring: two lands and two 6-drops in hand, so both go for basics
    @Test
    public void testRitesOfSpringTradesStuckSpells() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        addCard("Forest", ai);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        SpellAbility sa = addCardToZone("Rites of Spring", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(2), DiscardAnyAi.planned(sa));
    }

    // Rites of Refusal: they have 4 mana open, so two discards ({6}) counter their Serra Angel
    @Test
    public void testRitesOfRefusalPricesThemOut() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", opp);
        }
        SpellAbility sa = addCardToZone("Rites of Refusal", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Island", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility angel = addCardToZone("Serra Angel", opp, ZoneType.Hand).getFirstSpellAbility();
        angel.setActivatingPlayer(opp);
        game.getStack().add(angel);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(2), DiscardAnyAi.planned(sa));
    }
}
