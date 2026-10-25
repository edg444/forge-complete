package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 101. */
public class NoAiBatch101Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Swift Silence: their Wrath of God on the stack, nothing of its own
    @Test
    public void testSwiftSilenceCountersTheirSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        addCard("Island", ai);
        addCard("Island", ai);
        addCard("Island", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility wrath = addCardToZone("Wrath of God", opp, ZoneType.Hand).getFirstSpellAbility();
        wrath.setActivatingPlayer(opp);
        game.getStack().add(wrath);
        SpellAbility sa = addCardToZone("Swift Silence", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Uncage the Menagerie: the mana value its library has the most different creatures at
    @Test
    public void testUncageTheMenagerieX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Forest", ai);
        }
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Elvish Warrior", ai, ZoneType.Library);
        addCardToZone("Llanowar Elves", ai, ZoneType.Library);
        SpellAbility sa = addCardToZone("Uncage the Menagerie", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
    }
}
