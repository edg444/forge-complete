package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.TypeHurtAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 90. */
public class NoAiBatch90Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Roar of the Crowd: its four Goblins say four damage, to the face for lethal
    @Test
    public void testRoarOfTheCrowdForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
            addCard("Goblin Piker", ai);
        }
        opp.setLife(4, null);
        Card roar = addCardToZone("Roar of the Crowd", ai, ZoneType.Hand);
        SpellAbility sa = roar.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Goblin", TypeHurtAi.chosenType(roar));
        AssertJUnit.assertEquals(opp, sa.getSubAbility().getTargets().getFirstTargetedPlayer());
    }

    // Winding Way: creature or land, whichever its library has more of
    @Test
    public void testWindingWayNamesCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        addCard("Forest", ai);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        addCardToZone("Forest", ai, ZoneType.Library);
        Card way = addCardToZone("Winding Way", ai, ZoneType.Hand);
        SpellAbility sa = way.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Creature", TypeHurtAi.chosenType(way));
    }

    // Toxic Deluge: X paid in life, just enough for their creatures
    @Test
    public void testToxicDelugePaysLife() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Toxic Deluge", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(3, sa.getXManaCostPaid().intValue());
    }

    // Shimmering Mirage: cycled at their end step
    @Test
    public void testShimmeringMirageCycles() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        addCard("Island", ai);
        SpellAbility sa = addCardToZone("Shimmering Mirage", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
