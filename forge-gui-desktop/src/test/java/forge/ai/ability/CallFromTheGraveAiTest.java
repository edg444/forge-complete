package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Call from the Grave deals the moved creature's mana value to its caster, but that amount reads as 0 while the AI
 * decides (nothing is remembered yet), so it cast the spell at 2 life with full graveyards and killed itself.
 */
public class CallFromTheGraveAiTest extends AITest {

    private static boolean willCast(Player ai, int life) {
        ai.setLife(life, null);
        Card call = ai.getCardsIn(ZoneType.Hand).get(0);
        SpellAbility sa = call.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay();
    }

    private Game setUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Swamp", 3, ai);
        addCardToZone("Call from the Grave", ai, ZoneType.Hand);
        addCardToZone("Craw Wurm", opp, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    @Test
    public void doesNotCastWhenTheCostliestCreatureWouldKillIt() {
        Game game = setUp();
        AssertJUnit.assertFalse(willCast(game.getPlayers().get(1), 2));
    }

    @Test
    public void stillCastsWithLifeToSpare() {
        Game game = setUp();
        AssertJUnit.assertTrue(willCast(game.getPlayers().get(1), 20));
    }
}
