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

/** Second pass, batch 91. */
public class NoAiBatch91Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Turn to Frog: their Craw Wurm, blocking its Grizzly Bears, becomes a 1/1 the Bears kill
    @Test
    public void testTurnToFrogWinsTheFight() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Turn to Frog", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, wurm);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Djinn of Infinite Deceits: its Grizzly Bears for their Serra Angel
    @Test
    public void testDjinnTradesUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card djinn = addCard("Djinn of Infinite Deceits", ai);
        djinn.setSickness(false);
        Card bears = addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = null;
        for (SpellAbility s : djinn.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(angel));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
    }

    // Strip Bare: the Pacifism on its own Serra Angel
    @Test
    public void testStripBareFreesItsCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        Card angel = addCard("Serra Angel", ai);
        Card pacifism = addCard("Pacifism", opp);
        pacifism.attachToEntity(angel, null);
        SpellAbility sa = addCardToZone("Strip Bare", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }
}
