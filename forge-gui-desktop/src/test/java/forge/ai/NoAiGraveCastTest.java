package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** GraveCastAi: AILogic$ CastFromGraveyard. */
public class NoAiGraveCastTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Mizzix's Mastery: copy an instant or sorcery from its graveyard and cast it free - a Wrath of God on their
    // board, but not a Shock
    @Test
    public void testMizzixsMasteryRecastsWhatsWorthIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCardToZone("Shock", ai, ZoneType.Graveyard);
        SpellAbility sa = addCardToZone("Mizzix's Mastery", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp);
        addCard("Hill Giant", opp);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card wrath = addCardToZone("Wrath of God", ai, ZoneType.Graveyard);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wrath, sa.getTargetCard());
    }
}
