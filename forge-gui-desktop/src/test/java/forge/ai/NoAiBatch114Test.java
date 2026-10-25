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

/** Second pass, batch 114: AILogic$ SacRitual (Sacrifice, Burnt Offering, Metamorphosis). */
public class NoAiBatch114Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Sacrifice: its spare Hill Giant makes four black, enough for the Hypnotic Specter two Swamps can't cast
    @Test
    public void testSacrificeFuelsASpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Swamp", ai);
        addCard("Swamp", ai);
        addCard("Serra Angel", ai);
        SpellAbility sa = addCardToZone("Sacrifice", ai, ZoneType.Hand).getFirstSpellAbility();
        addCardToZone("Hypnotic Specter", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame("its Angel is worth more than the Specter's mana", AiPlayDecision.WillPlay, decide(ai, sa));

        Card giant = addCard("Hill Giant", ai);
        giant.setSVar("SacMe", "1");
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, SacrificePayoffAi.planned(sa).get(0));
    }
}
