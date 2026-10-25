package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 95. */
public class NoAiBatch95Test extends AITest {

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

    // Isochron Scepter: the free copy of its Lightning Bolt, when the AI would cast the Bolt now
    @Test
    public void testIsochronScepterCopiesWhenWorthIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Island", ai);
        }
        Card scepter = addCard("Isochron Scepter", ai);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Exile);
        scepter.addImprintedCard(bolt);
        bolt.setExiledWith(scepter);
        scepter.addExiledCard(bolt);
        opp.setLife(3, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, activated(scepter)));
    }
}
