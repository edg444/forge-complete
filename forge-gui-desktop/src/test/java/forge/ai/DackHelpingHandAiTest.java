package forge.ai;

import java.lang.reflect.Method;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Live games, 2026-11-07: Dack Fayden, Helping Hand gives each opponent a creature from your library, goaded. With one
 * opponent that's a creature attacking you every turn, so the AI casts it only with two or more opponents, and Flat
 * MC discovery doesn't add it back (its rollouts end before the goaded creature attacks).
 */
public class DackHelpingHandAiTest extends AITest {

    private Player ai;

    private Card dackInHand(Game game) {
        ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getPhaseHandler().onStackResolved();
        addCards("Plains", 6, ai);
        fillLibrary(ai, 10);
        addCardToZone("Hill Giant", ai, ZoneType.Library);
        return addCardToZone("Dack Fayden, Helping Hand", ai, ZoneType.Hand);
    }

    private AiPlayDecision decide(Card card) {
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test
    public void notWithOneOpponent() throws Exception {
        final Card dack = dackInHand(initAndCreateGame());
        AssertJUnit.assertEquals(AiPlayDecision.NeedsToPlayCriteriaNotMet, decide(dack));

        final AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        final Method legal = AiController.class.getDeclaredMethod("isLegalDiscovery", SpellAbility.class, AiPlayDecision.class);
        legal.setAccessible(true);
        final SpellAbility sa = dack.getFirstSpellAbility();
        AssertJUnit.assertEquals(Boolean.FALSE, legal.invoke(aic, sa, AiPlayDecision.NeedsToPlayCriteriaNotMet));
    }

    @Test
    public void withTwoOpponents() {
        final Card dack = dackInHand(initAndCreateThreePlayerGame());
        AssertJUnit.assertNotSame(AiPlayDecision.NeedsToPlayCriteriaNotMet, decide(dack));
    }
}
