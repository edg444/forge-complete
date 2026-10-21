package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * An Incident Has Occurred (UNF #143), checked against Scryfall and its rulings 2026-10-21: you may apologize whichever
 * modes you chose.
 */
public class AnIncidentHasOccurredTest extends AITest {

    @Test
    public void testDestroyFlyerAndApologize() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card angel = addCard("Serra Angel", opp);
        addCard("Forest", ai);
        addCard("Forest", ai);
        final Card spell = addCardToZone("An Incident Has Occurred", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sa.getDescription(), sa.getDescription().contains("You may apologize. If you do, you gain 3 life."));
        AssertJUnit.assertTrue(CharmEffect.makeChoices(sa));
        final int life = ai.getLife();
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertTrue(angel.isInZone(ZoneType.Graveyard) || game.getCardState(angel).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("the AI apologizes", life + 3, ai.getLife());
    }
}
