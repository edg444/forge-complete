package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Live game, 2026-11-07. The Monte Carlo AI activated Storm Elemental ("{U}, Exile the top card of your library: Tap
 * target creature with flying") on tapped creatures, one of them tapped by the ability already, and exiled its library
 * away: its discovered plays and other targets offered tapped creatures as Tap targets.
 */
public class StormElementalTapTargetTest extends AITest {

    private Game game;
    private Player ai;
    private Player opp;
    private SpellAbility tap;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        addCards("Island", 2, ai);
        fillLibrary(ai, 5);
        final Card elemental = addCard("Storm Elemental", ai);
        tap = elemental.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility() && sa.usesTargeting()).findFirst().get();
        tap.setActivatingPlayer(ai);
    }

    @Test
    public void tappedFlyerIsNoTarget() {
        setUp();
        addCard("Wind Drake", opp).setTapped(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNull(FlatMonteCarloPicker.hostileTarget(ai, tap));
    }

    @Test
    public void untappedFlyerIsTheTarget() {
        setUp();
        addCard("Serra Angel", opp).setTapped(true);
        final Card drake = addCard("Wind Drake", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertSame(drake, FlatMonteCarloPicker.hostileTarget(ai, tap));
    }

    @Test
    public void tappedFlyerIsNoAlternative() {
        setUp();
        final Card drake = addCard("Wind Drake", opp);
        final Card angel = addCard("Serra Angel", opp);
        angel.setTapped(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.applyTarget(tap, drake));
        final List<GameObject> others = FlatMonteCarloPicker.alternativeTargets(ai, tap, 4);
        AssertJUnit.assertFalse("a tapped creature can't be tapped again: " + others, others.contains(angel));
    }
}
