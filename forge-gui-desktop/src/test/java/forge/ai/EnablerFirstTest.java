package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.*;

public class EnablerFirstTest extends AITest {
    private Game game;
    private Player ai;
    private Player opp;

    private void setUp(final int islands, final String permanentInHand) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ((PlayerControllerAi) ai.getController()).getAi().setFlatMonteCarloOverride(false);
        addCards("Island", islands, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        bears.setCounters(CounterEnumType.P1P1, 1);
        // the Angel walls the 3/3, so bouncing it buys an attack: the heuristics cast Storm of Forms in Main 1
        addCard("Serra Angel", opp);
        addCard("Howling Mine", opp);
        addCardToZone("Storm of Forms", ai, ZoneType.Hand);
        addCardToZone(permanentInHand, ai, ZoneType.Hand);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
    }

    private String nextPlay() {
        final List<SpellAbility> chosen = ai.getController().chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    private List<String> castThisTurn() {
        final List<String> names = new ArrayList<>();
        for (final SpellAbility sa : game.getStack().getSpellsCastThisTurn()) {
            if (sa.getActivatingPlayer() == ai) {
                names.add(sa.getHostCard().getName());
            }
        }
        return names;
    }

    @Test(timeOut = 120_000)
    public void purseBeforeStormOfForms() {
        setUp(6, "Noble's Purse");
        assertEquals("Noble's Purse", nextPlay());
        playUntilPhase(game, PhaseType.END_OF_TURN);
        final List<String> cast = castThisTurn();
        assertTrue("both spells cast: " + cast, cast.contains("Noble's Purse") && cast.contains("Storm of Forms"));
        assertTrue("Purse first: " + cast, cast.indexOf("Noble's Purse") < cast.indexOf("Storm of Forms"));
    }

    @Test(timeOut = 120_000)
    public void stormFirstWhenThePermanentAddsNoCounterKind() {
        setUp(6, "Millstone");
        assertEquals("Storm of Forms", nextPlay());
    }

    @Test(timeOut = 120_000)
    public void stormFirstWhenBothCannotBePaidFor() {
        setUp(5, "Noble's Purse");
        assertEquals("Storm of Forms", nextPlay());
    }
}
