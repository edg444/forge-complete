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

/** Second pass, batch 102. */
public class NoAiBatch102Test extends AITest {

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

    // Sword of the Ages: just the Craw Wurm, its 6 power enough for their 6 life
    @Test
    public void testSwordOfTheAgesForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card sword = addCard("Sword of the Ages", ai);
        Card wurm = addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", ai);
        opp.setLife(6, null);
        SpellAbility sa = activated(sword);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(1, SacrificePayoffAi.planned(sa).size());
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));
    }

    // Vicious Betrayal: its unblocked Bears plus two sacrificed creatures' +4/+4 is lethal
    @Test
    public void testViciousBetrayalForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCard("Swamp", ai);
        }
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", ai);
        addCard("Llanowar Elves", ai);
        opp.setLife(6, null);
        SpellAbility sa = addCardToZone("Vicious Betrayal", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.setBlocked(bears, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
        AssertJUnit.assertEquals(2, SacrificePayoffAi.planned(sa).size());
    }
}
