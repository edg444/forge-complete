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

/** Second pass, batch 100. */
public class NoAiBatch100Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Hail Storm: two damage to their attacking Grizzly Bears pair, one to itself
    @Test
    public void testHailStormOnTheirAttackers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        Card b1 = addCard("Grizzly Bears", opp);
        Card b2 = addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Hail Storm", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(b1, ai);
        combat.addAttacker(b2, ai);
        combat.setBlocked(b1, false);
        combat.setBlocked(b2, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Immoral Bargain: its doomed Bears for their Serra Angel
    @Test
    public void testImmoralBargainTradesJunk() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Swamp", ai);
        addCard("Forest", ai);
        addCard("Forest", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Immoral Bargain", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(bears);
        game.getStack().add(blade);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(bears, SacrificePayoffAi.planned(sa).get(0));
    }
}
