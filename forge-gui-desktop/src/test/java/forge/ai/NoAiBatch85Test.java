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

/** Second pass, batch 85. */
public class NoAiBatch85Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Creeping Renaissance: the permanent type with the most in its graveyard
    @Test
    public void testCreepingRenaissanceNamesCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Forest", ai);
        }
        addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        addCardToZone("Forest", ai, ZoneType.Graveyard);
        Card renaissance = addCardToZone("Creeping Renaissance", ai, ZoneType.Hand);
        SpellAbility sa = renaissance.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Creature", TypeHurtAi.chosenType(renaissance));
    }

    // Corpse Lunge: the Craw Wurm card from its graveyard, for 6 damage to the Angel
    @Test
    public void testCorpseLungeExilesTheBigOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = addCardToZone("Corpse Lunge", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(wurm, forge.ai.ability.SacrificePayoffAi.planned(sa).get(0));
    }

    // Brightflame: X just big enough for their two green creatures
    @Test
    public void testBrightflamePicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 8; i++) {
            addCard("Mountain", ai);
        }
        addCard("Grizzly Bears", opp);
        addCard("Hill Giant", opp);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Brightflame", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        // Bears (green) and Craw Wurm (green) share a color: 4 kills both
        AssertJUnit.assertEquals(4, sa.getXManaCostPaid().intValue());
    }
}
