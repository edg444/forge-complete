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

/** RadianceAi, TypeHurtAi, and the random-discard cost (ComputerUtilCost.checkDiscardCost). */
public class NoAiRadianceTypeTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Cleansing Beam: 2 damage to target creature and each other creature sharing a color - not when that's its own
    @Test
    public void testCleansingBeamCountsTheWholeColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 2);
        addCard("Grizzly Bears", opp);
        addCard("Llanowar Elves", opp);
        SpellAbility sa = addCardToZone("Cleansing Beam", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        // two green creatures of its own die with them
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", ai);
        addCard("Fyndhorn Elves", ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Outbreak: "Choose a creature type. All creatures of that type get -1/-1" - the type that kills theirs
    @Test
    public void testOutbreakNamesTheirType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 4);
        addCard("Llanowar Elves", opp);
        addCard("Fyndhorn Elves", opp);
        addCard("Savannah Lions", ai);
        Card outbreak = addCardToZone("Outbreak", ai, ZoneType.Hand);
        SpellAbility sa = outbreak.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        // both of theirs are Elf Druids
        AssertJUnit.assertTrue(java.util.List.of("Elf", "Druid").contains(TypeHurtAi.chosenType(outbreak)));
    }

    // Frenetic Ogre: {R}, discard a card at random - only with a hand that's at least half spare
    @Test
    public void testRandomDiscardNeedsSpareCards() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Mountain", 7);
        Card ogre = addCard("Frenetic Ogre", ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        SpellAbility sa = null;
        for (SpellAbility s : ogre.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtilCost.checkDiscardCost(ai, sa.getPayCosts(), ogre, sa));

        addCardToZone("Mountain", ai, ZoneType.Hand);
        addCardToZone("Mountain", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(ComputerUtilCost.checkDiscardCost(ai, sa.getPayCosts(), ogre, sa));
    }
}
