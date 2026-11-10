package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Live game, 2026-11-10: the AI cast Mana Flair with three lands as the only permanents in play, for no mana.
 * The artist choice skipped the ritual check on its Mana sub-ability.
 */
public class ManaFlairAiTest extends AITest {

    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getPhaseHandler().onStackResolved();
    }

    private AiPlayDecision decide(Card card) {
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test
    public void onlyLandsInPlay() {
        setUp();
        addCards("Mountain", 3, ai);
        final Card flair = addCardToZone("Mana Flair", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(flair));
    }

    @Test
    public void oneNonlandPermanentIsLessThanItCosts() {
        setUp();
        addCards("Mountain", 3, ai);
        addCard("Ornithopter", ai);
        final Card flair = addCardToZone("Mana Flair", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(flair));
    }

    @Test
    public void threeByOneArtistReachesTheGiant() {
        setUp();
        addCards("Mountain", 3, ai);
        // same printing, same artist; the opponent's count too
        addCards("Ornithopter", 2, ai);
        addCard("Ornithopter", game.getPlayers().get(0));
        final Card flair = addCardToZone("Mana Flair", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(flair));
    }
}
