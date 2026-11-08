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
 * Evie Frye (ACR), checked against Scryfall 2026-11-07: "{1}, {T}: Draw a card, then discard a card." The script made
 * the discard optional, so the ability drew without discarding.
 */
public class EvieFryeDiscardTest extends AITest {

    @Test
    public void drawsThenDiscards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getPhaseHandler().onStackResolved();
        addCard("Island", ai);
        final Card evie = addCard("Evie Frye", ai);
        evie.setSickness(false);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        fillLibrary(ai, 5);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = evie.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        game.getStack().resolveStack();
        AssertJUnit.assertEquals("drew one, discarded one", 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Graveyard).size());
    }
}
