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

/** Second pass, batch 109: EndTurnAi (Time Stop, Sundial of the Infinite, Obeka). */
public class NoAiBatch109Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private void cast(Game game, Player p, String name) {
        SpellAbility s = addCardToZone(name, p, ZoneType.Hand).getFirstSpellAbility();
        s.setActivatingPlayer(p);
        game.getStack().add(s);
    }

    // Time Stop: their 4-drop on the stack is worth the card; an empty stack isn't
    @Test
    public void testTimeStopExilesTheirSpell() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = addCardToZone("Time Stop", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        cast(game, opp, "Hill Giant");
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Time Stop: their unblocked Wurm would kill it, so the turn ends before damage
    @Test
    public void testTimeStopSkipsALethalCombat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Time Stop", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Sundial of the Infinite: on its own turn, their flashed-in 4-drop goes; one of its own spells on the stack would go too
    @Test
    public void testSundialClearsTheirStack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Island", ai);
        SpellAbility sa = ability(addCard("Sundial of the Infinite", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        cast(game, opp, "Hill Giant");
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        cast(game, ai, "Craw Wurm");
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Obeka: on their turn the choice is theirs, so it isn't activated
    @Test
    public void testObekaOnlyOnItsOwnTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = ability(addCard("Obeka, Brute Chronologist", ai));
        cast(game, opp, "Hill Giant");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
