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

/** Second pass, batch 88. */
public class NoAiBatch88Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    // Mandate of Peace: their lethal attack ends with the combat phase
    @Test
    public void testMandateOfPeaceAgainstLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        addCard("Plains", ai);
        Card wurm = addCard("Craw Wurm", opp);
        ai.setLife(5, null);
        SpellAbility sa = addCardToZone("Mandate of Peace", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Martyr of Ashes: X red cards revealed - two kill their Bears and Elves, not its own Wall
    @Test
    public void testMartyrOfAshesPicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Mountain", ai);
        }
        Card martyr = addCard("Martyr of Ashes", ai);
        martyr.setSickness(false);
        addCard("Wall of Stone", ai);
        addCard("Grizzly Bears", opp);
        addCard("Llanowar Elves", opp);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        addCardToZone("Lava Axe", ai, ZoneType.Hand);
        SpellAbility sa = activated(martyr);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(2, sa.getXManaCostPaid().intValue());
    }

    // Meltdown: X for their artifacts, under its own
    @Test
    public void testMeltdownPicksX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCard("Mountain", ai);
        }
        addCard("Juggernaut", opp);
        addCard("Ornithopter", opp);
        SpellAbility sa = addCardToZone("Meltdown", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(4, sa.getXManaCostPaid().intValue());
    }
}
