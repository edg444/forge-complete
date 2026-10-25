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

/** Second pass, batch 104. */
public class NoAiBatch104Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Endemic Plague: its doomed Elf takes their two Elves with it
    @Test
    public void testEndemicPlagueSharesAType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        Card elf = addCard("Elvish Warrior", ai);
        addCard("Llanowar Elves", opp);
        addCard("Fyndhorn Elves", opp);
        SpellAbility sa = addCardToZone("Endemic Plague", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(elf);
        game.getStack().add(blade);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(elf, SacrificePayoffAi.planned(sa).get(0));
    }

    // Divine Reckoning: they keep one of three, it keeps its only one
    @Test
    public void testDivineReckoningOneSided() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", ai);
        }
        addCard("Serra Angel", ai);
        addCard("Craw Wurm", opp);
        addCard("Hill Giant", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Divine Reckoning", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
