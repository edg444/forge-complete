package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** SacrificePayoffAi: AILogic$ SacrificeFor and SacrificeAny. */
public class NoAiSacrificePayoffTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    private void doomBlade(Game game, Player opp, Card target) {
        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(target);
        game.getStack().add(blade);
    }

    // Carrion: "Create X 0/1 black Insect creature tokens, where X is the sacrificed creature's power." - not worth a
    // good creature, but a creature about to be destroyed anyway is free
    @Test
    public void testCarrionInAnswerToRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 3);
        Card wurm = addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Carrion", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        doomBlade(game, opp, wurm);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(1, SacrificePayoffAi.planned(sa).size());
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));

        // and the cost sacrifices the Wurm, not the cheaper Bears it would otherwise pick
        forge.game.cost.CostSacrifice sac = null;
        for (forge.game.cost.CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof forge.game.cost.CostSacrifice s) {
                sac = s;
            }
        }
        forge.game.cost.PaymentDecision paid = new AiCostDecision(ai, sa, false).visit(sac);
        AssertJUnit.assertEquals(1, paid.cards.size());
        AssertJUnit.assertEquals(wurm, paid.cards.get(0));
    }

    // Call for Blood: "Target creature gets -X/-X until end of turn, where X is the sacrificed creature's power."
    @Test
    public void testCallForBloodTurnsTheDoomedIntoAKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 5);
        Card wurm = addCard("Craw Wurm", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Call for Blood", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        doomBlade(game, opp, wurm);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));
    }

    // Last-Ditch Effort: "Sacrifice any number of creatures. Last-Ditch Effort deals that much damage to any target."
    @Test
    public void testLastDitchEffortForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 1);
        for (int i = 0; i < 3; i++) {
            addCard("Grizzly Bears", ai);
        }
        SpellAbility sa = addCardToZone("Last-Ditch Effort", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        opp.setLife(5, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(2, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getSubAbility().getTargets().getFirstTargetedPlayer());
        AssertJUnit.assertEquals(2, SacrificePayoffAi.planned(sa).size());
    }
}
